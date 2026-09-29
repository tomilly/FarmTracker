package pl.farmtracker.feature.fields.base

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Base
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.BaseLayer
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.testing.FakeBaseRepository
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.MainDispatcherRule

class BaseEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val silo = GeoPoint(51.95, 18.62)
    private val yard = GeoPoint(51.96, 18.63)

    // Leniwie: ViewModel startuje korutynę w init, więc musi powstać po podmianie Dispatchers.Main przez regułę.
    private val bases = FakeBaseRepository()
    private val viewModel by lazy { BaseEditorViewModel(bases, FakeFieldRepository()) }
    private val state get() = viewModel.uiState.value

    @Test
    fun `without a base the map waits for a tap on the photo`() {
        assertFalse(state.loading)
        assertNull(state.location)
        assertFalse(state.canSave)
        assertEquals(BaseLayer.PHOTO, viewModel.chrome.state.value.baseLayer)
        assertNull(viewModel.chrome.state.value.cameraRequest) // mapa pokaże „mnie"
    }

    @Test
    fun `tapping places the base and saving stores it`() = runTest {
        viewModel.onMapTapped(silo)
        viewModel.onMapTapped(yard) // przesunięcie

        assertTrue(state.canSave)
        viewModel.save()

        assertEquals(Base(yard), bases.base.first())
        assertTrue(state.done)
    }

    @Test
    fun `a saved base is shown up close and can be removed`() = runTest {
        bases.save(Base(silo))

        assertEquals(silo, state.location)
        assertFalse(state.canSave) // nic się nie zmieniło
        assertEquals(silo, (viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowPlace).point)

        viewModel.removeBase()
        viewModel.save()

        assertNull(bases.base.first())
    }
}
