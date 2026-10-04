package reframecv.ui.components.projects

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.ui.threading.runOnUiThread

class ProjectsStoreTest {
    @Test
    fun loadsOnlyWhenRequestedByIntent() = runOnUiThread {
        val executor = TestProjectsExecutor()
        val store = createProjectsStore(DefaultStoreFactory()) { executor }
        try {
            assertEquals(0, executor.loadCount)
            assertEquals(UiState.Loading, store.state)

            store.accept(ProjectsIntent.LoadProjects)

            assertEquals(1, executor.loadCount)
            assertEquals(UiState.NoProjects, store.state)
        } finally {
            store.dispose()
        }
    }
}
