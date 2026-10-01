package ankideckbuilder.ui.components.projects

import ankideckbuilder.ui.threading.runOnUiThread
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectsStoreTest {
    @Test
    fun loadsOnlyWhenRequestedByIntent() = runOnUiThread {
        val executor = TestProjectsExecutor()
        val store = createProjectsStore(DefaultStoreFactory()) { executor }
        try {
            assertEquals(0, executor.loadCount)

            store.accept(ProjectsIntent.LoadProjects)

            assertEquals(1, executor.loadCount)
            assertEquals(UiState.NoProjects, store.state)
        } finally {
            store.dispose()
        }
    }
}
