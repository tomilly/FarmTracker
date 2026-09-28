package pl.farmtracker.feature.map

import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeParcelRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.parcel.ParcelLookup

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val parcels = FakeParcelRepository()
    // Leniwie: ViewModel startuje korutynę w init, więc musi powstać po podmianie Dispatchers.Main przez regułę.
    private val viewModel by lazy { MapViewModel(parcels, FakeFieldRepository()) }
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
}
