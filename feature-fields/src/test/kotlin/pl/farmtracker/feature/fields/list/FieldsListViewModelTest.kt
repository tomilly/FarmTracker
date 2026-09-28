package pl.farmtracker.feature.fields.list

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.FieldColor
import pl.farmtracker.core.testing.FakeFieldRepository
import pl.farmtracker.core.testing.MainDispatcherRule
import pl.farmtracker.feature.fields.common.DeletedFieldBin

class FieldsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows fields from the repository and follows changes`() = runTest {
        val repository = FakeFieldRepository()
        val viewModel = FieldsListViewModel(repository, DeletedFieldBin())
        val field = Field(id = "1", name = "Za lasem", color = FieldColor.BLUE, shape = emptyList())

        viewModel.uiState.test {
            var item = awaitItem()
            if (item == FieldsListUiState.Loading) item = awaitItem()
            assertEquals(FieldsListUiState.Ready(emptyList()), item)

            repository.save(field)

            assertEquals(FieldsListUiState.Ready(listOf(field)), awaitItem())
        }
    }

    @Test
    fun `undo brings a deleted field back once`() = runTest {
        val repository = FakeFieldRepository()
        val bin = DeletedFieldBin()
        val viewModel = FieldsListViewModel(repository, bin)
        val field = Field(id = "1", name = "Za lasem", color = FieldColor.BLUE, shape = emptyList())
        bin.put(field)

        assertEquals(field, viewModel.recentlyDeleted.value)
        viewModel.undoDelete()
        viewModel.undoDelete()

        assertEquals(listOf(field), repository.fields.value)
        assertEquals(null, viewModel.recentlyDeleted.value)
    }

    @Test
    fun `dismissing forgets the deleted field`() {
        val bin = DeletedFieldBin()
        val viewModel = FieldsListViewModel(FakeFieldRepository(), bin)
        bin.put(Field(id = "1", name = "Za lasem", color = FieldColor.BLUE, shape = emptyList()))

        viewModel.dismissDeleted()

        assertEquals(null, viewModel.recentlyDeleted.value)
    }
}
