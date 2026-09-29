package pl.farmtracker.feature.fields.editor

import androidx.lifecycle.SavedStateHandle
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
import pl.farmtracker.core.domain.Place
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.domain.geo.bounds
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.FakeParcelRepository
import pl.farmtracker.core.testing.FakePlaceRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.parcel.ParcelLookup
import pl.farmtracker.data.parcel.ParcelSearch
import pl.farmtracker.data.place.PlaceSearch
import pl.farmtracker.feature.fields.common.DeletedFieldBin

class FieldEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val parcels = FakeParcelRepository()
    private val fields = FakeFieldRepository()
    private val places = FakePlaceRepository()

    // Leniwie: ViewModel musi powstać po podmianie Dispatchers.Main przez regułę.
    private val bin = DeletedFieldBin()
    private val viewModel by lazy { editor().also { it.chrome.onCameraIdle(16.0) } }

    private fun editor(fieldId: String? = null) = FieldEditorViewModel(
        parcels,
        places,
        fields,
        bin,
        SavedStateHandle(if (fieldId == null) emptyMap() else mapOf(FieldEditorViewModel.FIELD_ID_ARG to fieldId)),
    )
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
    fun `a tapped parcel is brought fully on screen without zooming in`() {
        tap(north, inNorth)

        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowArea
        assertEquals(north.shape.bounds(), request.bounds)
        assertFalse(request.zoomIn)
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

        viewModel.undoLast()

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
        assertFalse(state.done)
    }

    @Test
    fun `saving stores one field made of all selected parcels`() = runTest {
        tap(north, inNorth)
        tap(south, inSouth)
        viewModel.goToDetails()
        viewModel.onNameChanged("  Za lasem ")
        viewModel.onColorSelected(FieldColor.PURPLE)

        viewModel.save()

        assertTrue(state.done)
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

    @Test
    fun `drawing adds a corner on every tap, even without parcel boundaries`() {
        viewModel.chrome.onCameraIdle(10.0)
        viewModel.startDrawing()

        viewModel.onMapTapped(GeoPoint(50.0, 17.0))
        viewModel.onMapTapped(GeoPoint(50.0, 17.01))
        assertFalse(state.canContinue)

        viewModel.onMapTapped(GeoPoint(50.01, 17.01))

        assertEquals(ShapeMode.DRAW, state.shapeMode)
        assertEquals(3, state.drawnPoints.size)
        assertTrue(state.canContinue)
        assertTrue(state.areaHectares > 0)
        assertTrue(parcels.requests.isEmpty())
    }

    @Test
    fun `undo in drawing removes the last corner and picking parcels drops the drawing`() {
        viewModel.startDrawing()
        viewModel.onMapTapped(GeoPoint(50.0, 17.0))
        viewModel.onMapTapped(GeoPoint(50.0, 17.01))

        viewModel.undoLast()
        assertEquals(listOf(GeoPoint(50.0, 17.0)), state.drawnPoints)

        viewModel.stopDrawing()
        assertEquals(ShapeMode.PARCELS, state.shapeMode)
        assertTrue(state.drawnPoints.isEmpty())
    }

    @Test
    fun `drawing cannot start once parcels are selected`() {
        tap(north, inNorth)

        viewModel.startDrawing()

        assertEquals(ShapeMode.PARCELS, state.shapeMode)
    }

    @Test
    fun `drawn field is saved without parcels and with an empty suggested name`() = runTest {
        viewModel.startDrawing()
        listOf(GeoPoint(50.0, 17.0), GeoPoint(50.0, 17.01), GeoPoint(50.01, 17.01)).forEach(viewModel::onMapTapped)
        viewModel.goToDetails()
        assertEquals("", state.name)
        assertFalse(state.canSave)

        viewModel.onNameChanged("Klin przy rowie")
        viewModel.save()

        val saved = fields.fields.first().single()
        assertTrue(saved.parcelIds.isEmpty())
        assertEquals(3, saved.shape.single().outer.size)
    }

    @Test
    fun `several entries are marked on the map and saved with the field`() = runTest {
        tap(north, inNorth)
        viewModel.goToDetails()
        val gate = GeoPoint(50.0101, 17.0001)
        val backGate = GeoPoint(50.0199, 17.0099)

        viewModel.openEntry()
        assertTrue(viewModel.chrome.state.value.cameraRequest is CameraRequest.ShowArea)
        viewModel.onMapTapped(gate)
        viewModel.onMapTapped(backGate)
        viewModel.closeEntry()

        assertEquals(EditorStep.DETAILS, state.step)
        assertEquals(listOf(gate, backGate), state.entryPoints)
        assertEquals(listOf(north), state.parcels) // dotknięcie wjazdu nie zmienia działek

        viewModel.save()
        assertEquals(listOf(gate, backGate), fields.fields.first().single().entryPoints)
    }

    @Test
    fun `tapping an entry again removes it, undo drops the last one`() {
        tap(north, inNorth)
        viewModel.goToDetails()
        val gate = GeoPoint(50.0101, 17.0001)
        val backGate = GeoPoint(50.0199, 17.0099)
        viewModel.openEntry()
        viewModel.onMapTapped(gate)
        viewModel.onMapTapped(backGate)

        viewModel.onMapTapped(GeoPoint(50.01019, 17.0001)) // ok. 10 m obok – pod palcem
        assertEquals(listOf(backGate), state.entryPoints)

        viewModel.onMapTapped(gate)
        viewModel.undoEntry()
        assertEquals(listOf(backGate), state.entryPoints)

        viewModel.clearEntries()
        assertTrue(state.entryPoints.isEmpty())
    }

    @Test
    fun `defaults are set once so a chosen colour survives going back`() {
        tap(north, inNorth)
        viewModel.goToDetails()
        viewModel.onColorSelected(FieldColor.BROWN)

        viewModel.backToShape()
        viewModel.goToDetails()

        assertEquals(FieldColor.BROWN, state.color)
    }

    private val existing = Field(
        id = "f-1",
        name = "Za lasem",
        color = FieldColor.CYAN,
        shape = listOf(square(50.02, 17.0)),
        parcelIds = listOf("id-a", "id-b"),
        order = 4,
    )

    @Test
    fun `editing opens the form filled with the field`() = runTest {
        fields.save(existing)

        val edit = editor(fieldId = "f-1")

        val state = edit.uiState.value
        assertEquals(EditorStep.DETAILS, state.step)
        assertEquals("Za lasem", state.name)
        assertEquals(FieldColor.CYAN, state.color)
        assertEquals(existing.shape, state.fieldShape)
        assertFalse(state.loadingField)
    }

    @Test
    fun `marking the entry of an edited field shows that field, not my location`() = runTest {
        fields.save(existing)
        val edit = editor(fieldId = "f-1")

        edit.openEntry()
        edit.chrome.onStart(hasLocationPermission = true) // mapa pokazuje się dopiero w tym kroku

        val request = edit.chrome.state.value.cameraRequest as CameraRequest.ShowArea
        assertEquals(existing.shape.bounds(), request.bounds)
    }

    @Test
    fun `saving an edit keeps id, shape, parcels and order`() = runTest {
        fields.save(existing)
        val edit = editor(fieldId = "f-1")

        edit.onNameChanged("Przy lesie")
        edit.onColorSelected(FieldColor.PINK)
        edit.save()

        assertEquals(
            listOf(existing.copy(name = "Przy lesie", color = FieldColor.PINK)),
            fields.fields.first(),
        )
        assertTrue(edit.uiState.value.done)
    }

    @Test
    fun `deleting removes the field at once and keeps it for undo`() = runTest {
        fields.save(existing)
        val edit = editor(fieldId = "f-1")

        edit.delete()

        assertTrue(fields.fields.first().isEmpty())
        assertEquals(existing, bin.lastDeleted.value)
        assertTrue(edit.uiState.value.done)
    }

    @Test
    fun `editing a field that no longer exists just closes`() {
        val edit = editor(fieldId = "gone")

        assertTrue(edit.uiState.value.done)
    }

    @Test
    fun `a parcel already in another field is not added and the field is named`() = runTest {
        fields.save(existing.copy(parcelIds = listOf(north.id)))

        tap(north, inNorth)

        assertTrue(state.parcels.isEmpty())
        assertEquals(LookupProblem.ALREADY_USED, state.lastProblem)
        assertEquals("Za lasem", state.problemFieldName)
        assertEquals(0, state.pendingLookups)
    }

    @Test
    fun `picking a search result that belongs to another field shows it but does not add it`() = runTest {
        fields.save(existing.copy(parcelIds = listOf(north.id)))
        viewModel.openSearch()

        viewModel.pickSearchResult(north)

        assertEquals(EditorStep.SHAPE, state.step)
        assertTrue(state.parcels.isEmpty())
        assertEquals(LookupProblem.ALREADY_USED, state.lastProblem)
        assertTrue(viewModel.chrome.state.value.cameraRequest is CameraRequest.ShowArea)
    }

    private val sulmowNear = Place("Sulmów", "gmina Goszczanów", "powiat sieradzki", GeoPoint(50.01, 17.01))
    private val sulmowFar = Place("Sulmów", "gmina Kroczyce", "powiat zawierciański", GeoPoint(52.0, 19.0))

    @Test
    fun `a name without a number searches villages, nearest first, and asks no parcel service`() {
        places.result = PlaceSearch.Found(listOf(sulmowFar, sulmowNear))
        viewModel.chrome.onCameraIdle(15.0, GeoPoint(50.0, 17.0))
        viewModel.onSearchQueryChanged("sulmow")

        viewModel.runSearch()

        assertEquals(listOf("sulmow"), places.queries)
        assertTrue(parcels.searches.isEmpty())
        val hits = (state.search as SearchState.Places).hits
        assertEquals(listOf(sulmowNear, sulmowFar), hits.map { it.place })
    }

    @Test
    fun `picking a village moves the map there close enough to tap parcels`() {
        viewModel.openSearch()

        viewModel.pickPlace(sulmowNear)

        assertEquals(EditorStep.SHAPE, state.step)
        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowPlace
        assertEquals(sulmowNear.location, request.point)
    }

    @Test
    fun `parcel number with a village written without polish letters is corrected and retried`() {
        parcels.searchResult = ParcelSearch.NotFound
        places.result = PlaceSearch.Found(listOf(sulmowNear))
        viewModel.onSearchQueryChanged("sulmow 12")

        viewModel.runSearch()

        assertEquals(listOf("sulmow 12", "Sulmów 12"), parcels.searches)
        assertEquals(listOf("sulmow"), places.queries)
    }
}
