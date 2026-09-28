package pl.farmtracker.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelTest {

    private val viewModel = MapViewModel()
    private val state get() = viewModel.uiState.value

    @Test
    fun `starts on street map without parcels`() {
        assertEquals(BaseLayer.MAP, state.baseLayer)
        assertFalse(state.showParcels)
        assertEquals(LocationAccess.UNKNOWN, state.locationAccess)
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
    fun `parcels zoom hint only when parcels on and map zoomed out`() {
        viewModel.onZoomChanged(12.0)
        assertFalse(state.showParcelsZoomHint)

        viewModel.toggleParcels()
        assertTrue(state.showParcelsZoomHint)

        viewModel.onZoomChanged(17.0)
        assertFalse(state.showParcelsZoomHint)

        viewModel.toggleParcels()
        assertFalse(state.showParcels)
    }
}
