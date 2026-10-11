package reframecv.logging

import com.arkivanov.mvikotlin.core.store.Reducer
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import reframecv.domain.models.project.Project
import reframecv.ui.components.projects.ProjectsIntent
import reframecv.ui.components.projects.ProjectsLabel
import reframecv.ui.components.projects.UiState
import reframecv.ui.threading.runOnUiThread

private typealias HistoryExecutor =
    CoroutineExecutor<ProjectsIntent, Nothing, UiState, UiState, ProjectsLabel>

class HistoryStoreFactoryTest {
    @Test
    fun recordsLifecycleIntentsMessagesStatesAndLabelsWithoutPayloadText() = runOnUiThread {
        val history = EventHistory()
        val factory = createHistoryStoreFactory(history)
        val project = Project(12, "Private project name", 0, 0)
        val store: Store<ProjectsIntent, UiState, ProjectsLabel> = factory.create(
            name = "ProjectsStore(parentId=8)",
            initialState = UiState.Loading,
            executorFactory = {
                object : HistoryExecutor() {
                    override fun executeIntent(intent: ProjectsIntent) {
                        dispatch(UiState.Projects(listOf(project)))
                        publish(ProjectsLabel.Created(project))
                    }
                }
            },
            reducer = Reducer<UiState, UiState> { it },
        )
        try {
            store.accept(ProjectsIntent.CreateProject("Private new name"))
            assertEquals(UiState.Projects(listOf(project)), store.state)
        } finally {
            store.dispose()
        }
        val text = history.snapshot().joinToString("\n")
        listOf(
            "creating",
            "initializing",
            "INTENT CreateProject",
            "MESSAGE Projects(count=1)",
            "STATE Projects(count=1)",
            "LABEL Created(projectId=12)",
            "disposed",
        ).forEach {
            assertTrue(text.contains(it), "Missing event: $it")
        }
        assertFalse(text.contains("Private"))
    }
}
