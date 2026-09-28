package pl.farmtracker.core.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoBounds
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon

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
        assertEquals(CameraRequest.CenterOnMe(1), state.cameraRequest)
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
        assertEquals(CameraRequest.CenterOnMe(1), state.cameraRequest)
    }

    @Test
    fun `denying permission shows denied state and where-am-i asks again`() {
        chrome.onLocationPermissionResult(granted = false)
        assertEquals(LocationAccess.DENIED, state.locationAccess)

        chrome.onWhereAmIClicked()

        assertTrue(state.askForLocation)
        assertNull(state.cameraRequest)
    }

    @Test
    fun `where-am-i with permission requests centering each time`() {
        chrome.onStart(hasLocationPermission = true)

        chrome.onWhereAmIClicked()
        chrome.onWhereAmIClicked()

        assertEquals(CameraRequest.CenterOnMe(3), state.cameraRequest)
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
        chrome.onCameraIdle(12.0)
        assertFalse(state.showParcelsZoomHint)

        chrome.toggleParcels()
        assertTrue(state.showParcelsZoomHint)
        assertFalse(state.parcelsVisible)

        chrome.onCameraIdle(MapSources.PARCELS_MIN_ZOOM)
        assertFalse(state.showParcelsZoomHint)
        assertTrue(state.parcelsVisible)
    }

    @Test
    fun `showing an area is the newest camera request until where-am-i`() {
        chrome.onStart(hasLocationPermission = true)

        chrome.showArea(listOf(GeoPolygon(listOf(GeoPoint(50.0, 17.0), GeoPoint(50.01, 17.02)))))

        assertEquals(
            CameraRequest.ShowArea(2, GeoBounds(south = 50.0, west = 17.0, north = 50.01, east = 17.02)),
            state.cameraRequest,
        )

        chrome.onWhereAmIClicked()

        assertEquals(CameraRequest.CenterOnMe(3), state.cameraRequest)
    }

    @Test
    fun `camera idle remembers zoom and center`() {
        chrome.onCameraIdle(14.0, GeoPoint(50.98, 17.42))
        chrome.onCameraIdle(15.0)

        assertEquals(15.0, state.zoom, 0.0)
        assertEquals(GeoPoint(50.98, 17.42), state.center)
    }
}
