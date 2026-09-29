package pl.farmtracker.feature.fields.view

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.domain.geo.bounds
import pl.farmtracker.core.map.BaseLayer
import pl.farmtracker.core.map.CameraRequest
import pl.farmtracker.core.testing.FakeBaseRepository
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.MainDispatcherRule

class FieldViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun square(lat: Double, lon: Double) =
        GeoPolygon(listOf(GeoPoint(lat, lon), GeoPoint(lat, lon + 0.01), GeoPoint(lat + 0.01, lon + 0.01), GeoPoint(lat + 0.01, lon)))

    private val field = Field(id = "f-1", name = "Za lasem", color = FieldColor.CYAN, shape = listOf(square(50.0, 17.0)))
    private val neighbour = Field(id = "f-2", name = "Przy drodze", color = FieldColor.BLUE, shape = listOf(square(50.01, 17.0)))
    private val fields = FakeFieldRepository(listOf(field, neighbour))

    // Leniwie: ViewModel startuje korutynę w init, więc musi powstać po podmianie Dispatchers.Main przez regułę.
    private val viewModel by lazy { FieldViewModel(fields, FakeBaseRepository(), SavedStateHandle(mapOf(FieldViewModel.FIELD_ID_ARG to "f-1"))) }

    @Test
    fun `shows the whole field on the photo, with the fields around it`() {
        assertEquals(FieldViewUiState.Shown(field, listOf(field, neighbour)), viewModel.uiState.value)
        assertEquals(BaseLayer.PHOTO, viewModel.chrome.state.value.baseLayer)
        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowArea
        assertEquals(field.shape.bounds(), request.bounds)
    }

    @Test
    fun `changes from editing show up without moving the map again`() = runTest {
        val framing = viewModel.chrome.state.value.cameraRequest

        val renamed = field.copy(name = "Za lasem duże", entryPoints = listOf(GeoPoint(50.0, 17.0)))
        fields.save(renamed)

        assertEquals(renamed, (viewModel.uiState.value as FieldViewUiState.Shown).field)
        assertEquals(framing, viewModel.chrome.state.value.cameraRequest)
    }

    @Test
    fun `tapping another field on the map switches to it without zooming in`() {
        viewModel.onMapTapped(GeoPoint(50.015, 17.005))

        assertEquals(neighbour, (viewModel.uiState.value as FieldViewUiState.Shown).field)
        val request = viewModel.chrome.state.value.cameraRequest as CameraRequest.ShowArea
        assertEquals(neighbour.shape.bounds(), request.bounds)
        assertFalse(request.zoomIn)
    }

    @Test
    fun `tapping the same field or outside the fields changes nothing`() {
        val framing = viewModel.chrome.state.value.cameraRequest

        viewModel.onMapTapped(GeoPoint(50.005, 17.005))
        viewModel.onMapTapped(GeoPoint(51.0, 18.0))

        assertEquals(field, (viewModel.uiState.value as FieldViewUiState.Shown).field)
        assertEquals(framing, viewModel.chrome.state.value.cameraRequest)
    }

    @Test
    fun `a deleted field closes the screen`() = runTest {
        viewModel.uiState.value // start

        fields.delete("f-1")

        assertEquals(FieldViewUiState.Gone, viewModel.uiState.value)
    }
}
