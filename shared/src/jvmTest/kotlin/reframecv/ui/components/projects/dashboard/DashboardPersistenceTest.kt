package reframecv.ui.components.projects.dashboard

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.dependencies.DefaultApplicationDependencies
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class DashboardPersistenceTest : WorkspaceDatabaseTest() {
    @Test
    fun loadsAllSectionsFromDatabaseAndObservesSoftDeletion() = runBlocking<Unit> {
        val dependencies = DefaultApplicationDependencies { database }
        val projectId = project()
        val otherId = project("Other")
        val resumeId = dependencies.resumesRepository.create(resume(projectId))
        val vacancyId = dependencies.vacanciesRepository.create(vacancy(projectId))
        val letterId = dependencies.coverLettersRepository.create(letter(projectId))
        dependencies.resumesRepository.create(resume(otherId))
        val lifecycle = LifecycleRegistry()
        val states = Channel<DashboardState>(Channel.UNLIMITED)
        try {
            runOnUiThread {
                lifecycle.resume()
                val component = DefaultDashboardComponent(
                    DefaultAppComponentContext(DefaultComponentContext(lifecycle), dependencies),
                    projectId,
                )
                component.uiState.subscribe { states.trySend(it) }
            }
            withTimeout(10_000.milliseconds) {
                val loaded = states.awaitReady { it.resumes.isNotEmpty() }
                assertEquals(listOf(resumeId), loaded.resumes.map { it.id })
                assertEquals(listOf(vacancyId), loaded.vacancies.map { it.id })
                assertEquals(listOf(letterId), loaded.coverLetters.map { it.id })
                assertEquals(
                    listOf("Kotlin", "Android", "Résumé"),
                    loaded.resumes.single().keywords,
                )
                dependencies.resumesRepository.delete(resumeId)
                val updated = states.awaitReady { it.resumes.isEmpty() }
                assertEquals(loaded.vacancies, updated.vacancies)
                assertEquals(loaded.coverLetters, updated.coverLetters)
                dependencies.projectsRepository.deleteProject(projectId)
                assertEquals(
                    DashboardState.Ready(),
                    states.awaitReady {
                        it.resumes.isEmpty() && it.vacancies.isEmpty() && it.coverLetters.isEmpty()
                    },
                )
            }
        } finally {
            runOnUiThread { lifecycle.destroy() }
            states.close()
        }
    }

    private suspend fun Channel<DashboardState>.awaitReady(
        predicate: (DashboardState.Ready) -> Boolean,
    ): DashboardState.Ready {
        while (true) {
            val state = receive()
            if (state is DashboardState.Ready && predicate(state)) return state
        }
    }
}
