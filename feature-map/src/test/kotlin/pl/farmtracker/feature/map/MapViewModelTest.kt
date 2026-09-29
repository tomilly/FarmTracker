package pl.farmtracker.feature.map

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.PositionReport
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.geo.GeoBounds
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.domain.geo.bounds
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.map.MapPerson
import pl.farmtracker.core.testing.FakeBaseRepository
import pl.farmtracker.core.testing.FakeClock
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeLiveLocationRepository
import pl.farmtracker.core.testing.FakeParcelRepository
import pl.farmtracker.core.testing.FakeSessionRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.parcel.ParcelLookup

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val parcels = FakeParcelRepository()
    // Leniwie: ViewModel startuje korutynę w init, więc musi powstać po podmianie Dispatchers.Main przez regułę.
    private val session = FakeSessionRepository(Role.DRIVER)
    private val fieldsRepo = FakeFieldRepository()
    private val locations = FakeLiveLocationRepository(myRole = Role.DRIVER)
    private val clock = FakeClock(now = 1_000_000)
    private val viewModel by lazy {
        MapViewModel(parcels, fieldsRepo, session, FakeBaseRepository(), locations, clock)
    }
    private val selection get() = viewModel.parcelSelection.value

    private val tapPoint = GeoPoint(latitude = 50.98, longitude = 17.42)
    private val parcel = Parcel(
        id = "160802_2.0012.345",
        number = "345",
        precinct = "Sulmów",
        commune = "Olszanka",
        shape = listOf(GeoPolygon(listOf(GeoPoint(50.98, 17.42), GeoPoint(50.98, 17.43), GeoPoint(50.99, 17.43)))),
    )

    private fun showParcelsZoomedIn() {
        viewModel.chrome.toggleParcels()
        viewModel.chrome.onCameraIdle(15.0)
    }

    @Test
    fun `nothing is selected at start`() {
        assertEquals(ParcelSelection.None, selection)
    }

    @Test
    fun `tapping a parcel selects it`() {
        parcels.result = ParcelLookup.Found(parcel)
        showParcelsZoomedIn()

        viewModel.onMapTapped(tapPoint)

        assertEquals(listOf(tapPoint), parcels.requests)
        assertEquals(parcel, selection.selectedParcel)
    }

    @Test
    fun `tapping a parcel moves the map onto it without zooming in`() {
        parcels.result = ParcelLookup.Found(parcel)
        showParcelsZoomedIn()

        viewModel.onMapTapped(tapPoint)

        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowArea
        assertEquals(GeoBounds(south = 50.98, west = 17.42, north = 50.99, east = 17.43), request.bounds)
        assertFalse(request.zoomIn)
    }

    @Test
    fun `while looking up the parcel the map says it is searching`() {
        val gate = CompletableDeferred<Unit>()
        parcels.result = ParcelLookup.Found(parcel)
        parcels.gate = gate
        showParcelsZoomedIn()

        viewModel.onMapTapped(tapPoint)
        assertEquals(ParcelSelection.Searching, selection)

        gate.complete(Unit)
        assertEquals(ParcelSelection.Selected(parcel), selection)
    }

    @Test
    fun `no parcel or no signal is reported, not a crash`() {
        showParcelsZoomedIn()

        parcels.result = ParcelLookup.NotFound
        viewModel.onMapTapped(tapPoint)
        assertEquals(ParcelSelection.NotFound, selection)

        parcels.result = ParcelLookup.Unavailable
        viewModel.onMapTapped(tapPoint)
        assertEquals(ParcelSelection.Unavailable, selection)
    }

    @Test
    fun `taps are ignored when parcel boundaries are not visible`() {
        viewModel.chrome.onCameraIdle(15.0)
        viewModel.onMapTapped(tapPoint) // działki wyłączone

        viewModel.chrome.toggleParcels()
        viewModel.chrome.onCameraIdle(10.0)
        viewModel.onMapTapped(tapPoint) // za daleko

        assertTrue(parcels.requests.isEmpty())
        assertEquals(ParcelSelection.None, selection)
    }

    @Test
    fun `closing or hiding parcels clears the selection`() {
        parcels.result = ParcelLookup.Found(parcel)
        showParcelsZoomedIn()
        viewModel.onMapTapped(tapPoint)

        viewModel.clearParcelSelection()
        assertNull(selection.selectedParcel)

        viewModel.onMapTapped(tapPoint)
        viewModel.chrome.toggleParcels()
        assertNull(selection.selectedParcel)
    }

    private val field = Field(
        id = "f-1",
        name = "Za lasem",
        color = FieldColor.ORANGE,
        shape = listOf(GeoPolygon(listOf(GeoPoint(51.0, 17.0), GeoPoint(51.0, 17.01), GeoPoint(51.01, 17.01), GeoPoint(51.01, 17.0)))),
    )
    private val inField = GeoPoint(51.005, 17.005)

    private fun TestScope.watchSelection() {
        viewModel.selectedField.launchIn(backgroundScope)
        viewModel.canEditFields.launchIn(backgroundScope)
    }

    @Test
    fun `tapping a field shows it instead of looking up a parcel`() = runTest(mainDispatcherRule.testDispatcher) {
        fieldsRepo.save(field)
        watchSelection()
        showParcelsZoomedIn()

        viewModel.onMapTapped(inField)

        assertEquals(field, viewModel.selectedField.value)
        assertTrue(parcels.requests.isEmpty())
        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowArea
        assertEquals(field.shape.bounds(), request.bounds)
        assertFalse(request.zoomIn)
    }

    @Test
    fun `tapping outside the fields or closing the card drops the field`() = runTest(mainDispatcherRule.testDispatcher) {
        fieldsRepo.save(field)
        watchSelection()

        viewModel.onMapTapped(inField)
        viewModel.onMapTapped(tapPoint)
        assertNull(viewModel.selectedField.value)

        viewModel.onMapTapped(inField)
        viewModel.clearFieldSelection()
        assertNull(viewModel.selectedField.value)
    }

    @Test
    fun `only the admin can edit a field from the map`() = runTest(mainDispatcherRule.testDispatcher) {
        watchSelection()
        assertFalse(viewModel.canEditFields.value)

        session.setRole(Role.ADMIN)

        assertTrue(viewModel.canEditFields.value)
    }

    private fun someone(name: String, role: Role, minutesAgo: Int) =
        LiveLocation(name, name, role, inField, timeMillis = clock.now - minutesAgo * 60_000L)

    @Test
    fun `the map shows the others at work - old positions greyed, forgotten ones gone`() =
        runTest(mainDispatcherRule.testDispatcher) {
            locations.locations.value = listOf(
                someone("Rysiek", Role.HARVESTER, minutesAgo = 0),
                someone("Marek", Role.DRIVER, minutesAgo = 5),
                someone("Janek", Role.DRIVER, minutesAgo = 13 * 60),
            )
            locations.publish(PositionReport(tapPoint, fieldId = null, timeMillis = clock.now))
            viewModel.people.launchIn(backgroundScope)

            assertEquals(
                listOf(
                    MapPerson(inField, "Rysiek", Role.HARVESTER, isStale = false),
                    MapPerson(inField, "Marek", Role.DRIVER, isStale = true),
                ),
                viewModel.people.value,
            )
        }
}
