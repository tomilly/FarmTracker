package pl.farmtracker.feature.fields.editor

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeParcelRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelSearch

class FieldEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val parcels = FakeParcelRepository()
    private val fields = FakeFieldRepository()

    // Leniwie: ViewModel musi powstać po podmianie Dispatchers.Main przez regułę.
    private val viewModel by lazy { FieldEditorViewModel(parcels, fields).also { it.chrome.onCameraIdle(16.0) } }
    private val state get() = viewModel.uiState.value

    private fun square(lat: Double, lon: Double) =
        GeoPolygon(listOf(GeoPoint(lat, lon), GeoPoint(lat, lon + 0.01), GeoPoint(lat + 0.01, lon + 0.01), GeoPoint(lat + 0.01, lon)))

    private val north = Parcel("id-north", "12", "Bystrzyca", "Oława", listOf(square(50.01, 17.0)))
    private val south = Parcel("id-south", "13/2", "Bystrzyca", "Oława", listOf(square(50.0, 17.0)))
    private val inNorth = GeoPoint(50.015, 17.005)
    private val inSouth = GeoPoint(50.005, 17.005)

    private fun tap(parcel: Parcel, point: GeoPoint) {
        parcels.result = ParcelLookup.Found(parcel)
        viewModel.onMapTapped(point)
    }

    @Test
    fun `starts on the photo with parcel boundaries`() {
        assertTrue(viewModel.chrome.state.value.showParcels)
        assertEquals(EditorStep.SHAPE, state.step)
        assertFalse(state.canContinue)
    }

    @Test
    fun `tapping parcels adds them and sums the area`() {
        tap(north, inNorth)
        tap(south, inSouth)

        assertEquals(listOf(north, south), state.parcels)
        assertEquals(north.areaHectares + south.areaHectares, state.areaHectares, 0.0001)
        assertTrue(state.canContinue)
    }

    @Test
    fun `tapping a selected parcel again removes it without asking the server`() {
        tap(north, inNorth)
        parcels.requests.clear()

        viewModel.onMapTapped(GeoPoint(50.012, 17.002))

        assertTrue(state.parcels.isEmpty())
        assertTrue(parcels.requests.isEmpty())
    }

    @Test
    fun `the same parcel is not added twice`() {
        tap(north, inNorth)
        tap(north, GeoPoint(50.5, 17.5)) // np. serwer zwrócił tę samą działkę dla punktu na granicy

        assertEquals(listOf(north), state.parcels)
    }

    @Test
    fun `undo removes the last added parcel`() {
        tap(north, inNorth)
        tap(south, inSouth)

        viewModel.removeLastParcel()

        assertEquals(listOf(north), state.parcels)
    }

    @Test
    fun `lookup problems are shown and cleared by the next tap`() {
        parcels.result = ParcelLookup.Unavailable
        viewModel.onMapTapped(inNorth)
        assertEquals(LookupProblem.UNAVAILABLE, state.lastProblem)

        parcels.result = ParcelLookup.NotFound
        viewModel.onMapTapped(inNorth)
        assertEquals(LookupProblem.NOT_FOUND, state.lastProblem)

        tap(north, inNorth)
        assertNull(state.lastProblem)
        assertEquals(0, state.pendingLookups)
    }

    @Test
    fun `taps are ignored when parcels are too small to see`() {
        viewModel.chrome.onCameraIdle(10.0)

        viewModel.onMapTapped(inNorth)

        assertTrue(parcels.requests.isEmpty())
    }

    @Test
    fun `next suggests a name and a colour not used by other fields`() = runTest {
        fields.save(Field("x", "Stare", FieldColor.BLUE, listOf(square(49.0, 16.0))))
        tap(south, inSouth)

        viewModel.goToDetails()

        assertEquals(EditorStep.DETAILS, state.step)
        assertEquals("Bystrzyca 13/2", state.name)
        assertEquals(FieldColor.ORANGE, state.color)
    }

    @Test
    fun `going back keeps the selection and the typed name`() {
        tap(north, inNorth)
        viewModel.goToDetails()
        viewModel.onNameChanged("Za lasem")

        viewModel.backToShape()
        viewModel.goToDetails()

        assertEquals(listOf(north), state.parcels)
        assertEquals("Za lasem", state.name)
    }

    @Test
    fun `blank name cannot be saved`() {
        tap(north, inNorth)
        viewModel.goToDetails()

        viewModel.onNameChanged("   ")
        viewModel.save()

        assertFalse(state.canSave)
        assertFalse(state.saved)
    }

    @Test
    fun `saving stores one field made of all selected parcels`() = runTest {
        tap(north, inNorth)
        tap(south, inSouth)
        viewModel.goToDetails()
        viewModel.onNameChanged("  Za lasem ")
        viewModel.onColorSelected(FieldColor.PURPLE)

        viewModel.save()

        assertTrue(state.saved)
        val saved = fields.fields.first().single()
        assertEquals("Za lasem", saved.name)
        assertEquals(FieldColor.PURPLE, saved.color)
        assertEquals(listOf("id-north", "id-south"), saved.parcelIds)
        assertEquals(north.shape + south.shape, saved.shape)
    }

    @Test
    fun `search results are sorted from the nearest to the map center`() {
        val far = south.copy(id = "far", shape = listOf(square(54.0, 18.0)))
        parcels.searchResult = ParcelSearch.Found(listOf(far, north))
        viewModel.chrome.onCameraIdle(15.0, GeoPoint(50.0, 17.0))
        viewModel.openSearch()
        viewModel.onSearchQueryChanged(" Bystrzyca 12 ")

        viewModel.runSearch()

        assertEquals(listOf(" Bystrzyca 12 ".trim()), parcels.searches)
        val hits = (state.search as SearchState.Results).hits
        assertEquals(listOf("id-north", "far"), hits.map { it.parcel.id })
        assertTrue(hits.first().distanceKm!! < 3.0)
    }

    @Test
    fun `picking a result adds the parcel, goes back to the map and shows it`() {
        parcels.searchResult = ParcelSearch.Found(listOf(north))
        viewModel.openSearch()
        viewModel.onSearchQueryChanged("Bystrzyca 12")
        viewModel.runSearch()

        viewModel.pickSearchResult(north)

        assertEquals(EditorStep.SHAPE, state.step)
        assertEquals(listOf(north), state.parcels)
        assertTrue(viewModel.chrome.state.value.cameraRequest is CameraRequest.ShowArea)
    }

    @Test
    fun `search problems are reported and blank query is not sent`() {
        viewModel.openSearch()
        viewModel.runSearch()
        assertTrue(parcels.searches.isEmpty())

        viewModel.onSearchQueryChanged("Nie ma 1")
        parcels.searchResult = ParcelSearch.NotFound
        viewModel.runSearch()
        assertEquals(SearchState.NotFound, state.search)

        parcels.searchResult = ParcelSearch.Unavailable
        viewModel.runSearch()
        assertEquals(SearchState.Unavailable, state.search)
    }
}
