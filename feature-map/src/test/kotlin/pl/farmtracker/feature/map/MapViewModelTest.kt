package pl.farmtracker.feature.map

import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.testing.FakeParcelRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.data.parcel.ParcelLookup

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val parcels = FakeParcelRepository()
    private val viewModel = MapViewModel(parcels)
    private val state get() = viewModel.uiState.value

    private val tapPoint = GeoPoint(latitude = 50.98, longitude = 17.42)
    private val parcel = Parcel(
        id = "160802_2.0012.345",
        number = "345",
        precinct = "Sulmów",
        commune = "Olszanka",
        shape = listOf(GeoPolygon(listOf(GeoPoint(50.98, 17.42), GeoPoint(50.98, 17.43), GeoPoint(50.99, 17.43)))),
    )

    private fun showParcelsZoomedIn() {
        viewModel.toggleParcels()
        viewModel.onZoomChanged(15.0)
    }

    @Test
    fun `starts on street map without parcels`() {
        assertEquals(BaseLayer.MAP, state.baseLayer)
        assertFalse(state.showParcels)
        assertEquals(LocationAccess.UNKNOWN, state.locationAccess)
        assertEquals(ParcelSelection.None, state.parcelSelection)
    }

    @Test
    fun `with permission the map centers on me right away`() {
        viewModel.onStart(hasLocationPermission = true)

        assertEquals(LocationAccess.GRANTED, state.locationAccess)
        assertEquals(1, state.centerOnMeRequest)
        assertFalse(state.askForLocation)
    }

    @Test
    fun `without permission the map asks once`() {
        viewModel.onStart(hasLocationPermission = false)
        assertTrue(state.askForLocation)

        viewModel.onLocationPermissionAsked()
        viewModel.onStart(hasLocationPermission = false) // np. obrót ekranu

        assertFalse(state.askForLocation)
    }

    @Test
    fun `granting permission centers on me`() {
        viewModel.onStart(hasLocationPermission = false)
        viewModel.onLocationPermissionAsked()

        viewModel.onLocationPermissionResult(granted = true)

        assertEquals(LocationAccess.GRANTED, state.locationAccess)
        assertEquals(1, state.centerOnMeRequest)
    }

    @Test
    fun `denying permission shows denied state and where-am-i asks again`() {
        viewModel.onLocationPermissionResult(granted = false)
        assertEquals(LocationAccess.DENIED, state.locationAccess)

        viewModel.onWhereAmIClicked()

        assertTrue(state.askForLocation)
        assertEquals(0, state.centerOnMeRequest)
    }

    @Test
    fun `where-am-i with permission requests centering each time`() {
        viewModel.onStart(hasLocationPermission = true)

        viewModel.onWhereAmIClicked()
        viewModel.onWhereAmIClicked()

        assertEquals(3, state.centerOnMeRequest)
    }

    @Test
    fun `permission granted in system settings is picked up on return`() {
        viewModel.onLocationPermissionResult(granted = false)

        viewModel.onLocationPermissionRechecked(granted = true)

        assertEquals(LocationAccess.GRANTED, state.locationAccess)
    }

    @Test
    fun `switching base layer`() {
        viewModel.selectBaseLayer(BaseLayer.PHOTO)

        assertEquals(BaseLayer.PHOTO, state.baseLayer)
    }

    @Test
    fun `parcels are visible from a whole-field zoom, with a hint below it`() {
        viewModel.onZoomChanged(12.0)
        assertFalse(state.showParcelsZoomHint)

        viewModel.toggleParcels()
        assertTrue(state.showParcelsZoomHint)
        assertFalse(state.parcelsVisible)

        viewModel.onZoomChanged(MapSources.PARCELS_MIN_ZOOM)
        assertFalse(state.showParcelsZoomHint)
        assertTrue(state.parcelsVisible)
    }

    @Test
    fun `tapping a parcel selects it`() {
        parcels.result = ParcelLookup.Found(parcel)
        showParcelsZoomedIn()

        viewModel.onMapTapped(tapPoint)

        assertEquals(listOf(tapPoint), parcels.requests)
        assertEquals(parcel, state.selectedParcel)
    }

    @Test
    fun `while looking up the parcel the map says it is searching`() {
        val gate = CompletableDeferred<Unit>()
        parcels.result = ParcelLookup.Found(parcel)
        parcels.gate = gate
        showParcelsZoomedIn()

        viewModel.onMapTapped(tapPoint)
        assertEquals(ParcelSelection.Searching, state.parcelSelection)

        gate.complete(Unit)
        assertEquals(ParcelSelection.Selected(parcel), state.parcelSelection)
    }

    @Test
    fun `no parcel or no signal is reported, not a crash`() {
        showParcelsZoomedIn()

        parcels.result = ParcelLookup.NotFound
        viewModel.onMapTapped(tapPoint)
        assertEquals(ParcelSelection.NotFound, state.parcelSelection)

        parcels.result = ParcelLookup.Unavailable
        viewModel.onMapTapped(tapPoint)
        assertEquals(ParcelSelection.Unavailable, state.parcelSelection)
    }

    @Test
    fun `taps are ignored when parcel boundaries are not visible`() {
        viewModel.onZoomChanged(15.0)
        viewModel.onMapTapped(tapPoint) // działki wyłączone

        viewModel.toggleParcels()
        viewModel.onZoomChanged(10.0)
        viewModel.onMapTapped(tapPoint) // za daleko

        assertTrue(parcels.requests.isEmpty())
        assertEquals(ParcelSelection.None, state.parcelSelection)
    }

    @Test
    fun `closing or hiding parcels clears the selection`() {
        parcels.result = ParcelLookup.Found(parcel)
        showParcelsZoomedIn()
        viewModel.onMapTapped(tapPoint)

        viewModel.clearParcelSelection()
        assertNull(state.selectedParcel)

        viewModel.onMapTapped(tapPoint)
        viewModel.toggleParcels()
        assertNull(state.selectedParcel)
        assertFalse(state.showParcels)
    }
}
