package reframecv.logging

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.dependencies.ApplicationDependencies
import reframecv.dependencies.DefaultApplicationDependencies
import reframecv.domain.models.project.Project
import reframecv.repository.ProjectsRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.DefaultProjectsComponent
import reframecv.ui.components.projects.editor.EditorSaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProjectsUseCases

class ErrorHistoryIntegrationTest : ComponentTest() {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun componentFailureWritesStoreHistoryThroughApplicationDependencies() = runBlocking<Unit> {
        val history = EventHistory()
        val file = temporaryFolder.root.toPath().resolve("stacktrace.txt")
        val services = DefaultApplicationDependencies(
            eventHistory = history,
            logger = FileApplicationLogger(file, history),
            databaseFactory = { error("This test must not open a database") },
        )
        val repository = object : ProjectsRepository by dependencies.projectsRepository {
            override suspend fun createProject(name: String, parentId: Long?): Project =
                error("Test storage failure")
        }
        val context = DefaultAppComponentContext(
            DefaultComponentContext(lifecycle),
            object : ApplicationDependencies by services {
                override val projectsUseCases get() = ProjectsUseCases(projectsRepository)
                override val projectsRepository = repository
            },
        )
        val states = Channel<EditorSaveState>(Channel.UNLIMITED)
        try {
            runOnUiThread {
                lifecycle.resume()
                val component = DefaultProjectsComponent(context)
                component.onAddProject()
                val editor = requireNotNull(component.editorSlot.value.child).instance
                editor.saveState.subscribe { states.trySend(it) }
                editor.onSave("Private project name")
            }
            withTimeout(10.seconds) {
                while (states.receive() != EditorSaveState.Failed) { /* Wait for the report. */ }
            }
            val text = Files.readString(file)
            assertContains(text, "ERROR Create project {parentId=null}")
            assertContains(text, "ProjectsStore(parentId=null) INTENT CreateProject")
            assertContains(text, "ProjectsStore(parentId=null) LABEL Saving")
            assertTrue(text.indexOf("INTENT CreateProject") < text.indexOf("Stack trace:"))
            assertContains(text, "java.lang.IllegalStateException: Test storage failure")
            assertFalse(text.contains("Private project name"))
        } finally {
            services.close()
        }
    }
}
