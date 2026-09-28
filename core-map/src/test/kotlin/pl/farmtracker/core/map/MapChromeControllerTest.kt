package pl.farmtracker.core.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapChromeControllerTest {

    private val chrome = MapChromeController()
    private val state get() = chrome.state.value

    @Test
    fun `starts on street map without parcels`() {
        assertEquals(BaseLayer.MAP, state.baseLayer)
        assertFalse(state.showParcels)
        assertEquals(LocationAccess.UNKNOWN, state.locationAccess)
    }

    @Test
    fun `with permission the map centers on me right away`() {
        chrome.onStart(hasLocationPermission = true)

        assertEquals(LocationAccess.GRANTED, state.locationAccess)
        assertEquals(1, state.centerOnMeRequest)
        assertFalse(state.askForLocation)
    }

    @Test
    fun `without permission the map asks once`() {
        chrome.onStart(hasLocationPermission = false)
        assertTrue(state.askForLocation)

        chrome.onLocationPermissionAsked()
        chrome.onStart(hasLocationPermission = false) // np. obrót ekranu

        assertFalse(state.askForLocation)
    }

    @Test
    fun `granting permission centers on me`() {
        chrome.onStart(hasLocationPermission = false)
        chrome.onLocationPermissionAsked()

        chrome.onLocationPermissionResult(granted = true)

        assertEquals(LocationAccess.GRANTED, state.locationAccess)
        assertEquals(1, state.centerOnMeRequest)
    }

    @Test
    fun `denying permission shows denied state and where-am-i asks again`() {
        chrome.onLocationPermissionResult(granted = false)
        assertEquals(LocationAccess.DENIED, state.locationAccess)

        chrome.onWhereAmIClicked()

        assertTrue(state.askForLocation)
        assertEquals(0, state.centerOnMeRequest)
    }

    @Test
    fun `where-am-i with permission requests centering each time`() {
        chrome.onStart(hasLocationPermission = true)

        chrome.onWhereAmIClicked()
        chrome.onWhereAmIClicked()

        assertEquals(3, state.centerOnMeRequest)
    }

    @Test
    fun `permission granted in system settings is picked up on return`() {
        chrome.onLocationPermissionResult(granted = false)

        chrome.onLocationPermissionRechecked(granted = true)

        assertEquals(LocationAccess.GRANTED, state.locationAccess)
    }

    @Test
    fun `switching base layer`() {
        chrome.selectBaseLayer(BaseLayer.PHOTO)

        assertEquals(BaseLayer.PHOTO, state.baseLayer)
    }

    @Test
    fun `parcels are visible from a whole-field zoom, with a hint below it`() {
        chrome.onZoomChanged(12.0)
        assertFalse(state.showParcelsZoomHint)

        chrome.toggleParcels()
        assertTrue(state.showParcelsZoomHint)
        assertFalse(state.parcelsVisible)

        chrome.onZoomChanged(MapSources.PARCELS_MIN_ZOOM)
        assertFalse(state.showParcelsZoomHint)
        assertTrue(state.parcelsVisible)
    }
}
