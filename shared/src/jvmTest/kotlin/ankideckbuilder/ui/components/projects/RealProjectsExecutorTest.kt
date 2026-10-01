package ankideckbuilder.ui.components.projects

import ankideckbuilder.ui.threading.runOnUiThread
import com.arkivanov.mvikotlin.core.store.Executor
import kotlin.test.Test
import kotlin.test.assertEquals

class RealProjectsExecutorTest {
    @Test
    fun emitsEmptyProjectsStateOnLoad() = runOnUiThread {
        val messages = mutableListOf<UiState>()
        val executor = RealProjectsExecutor()
        try {
            executor.init(object : Executor.Callbacks<UiState, UiState, Nothing, Nothing> {
                override val state: UiState = UiState.NoProjects

                override fun onMessage(message: UiState) {
                    messages += message
                }

                override fun onAction(action: Nothing) = Unit

                override fun onLabel(label: Nothing) = Unit
            })

            assertEquals(emptyList(), messages)
            executor.executeIntent(ProjectsIntent.LoadProjects)

            assertEquals(listOf<UiState>(UiState.NoProjects), messages)
        } finally {
            executor.dispose()
        }
    }
}
