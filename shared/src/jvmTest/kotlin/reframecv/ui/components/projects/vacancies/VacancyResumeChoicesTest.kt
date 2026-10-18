package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.repository.ResumesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyResumesComponent
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ResumesUseCases

class VacancyResumeChoicesTest : ComponentTest() {
    @Test
    fun retriesFailedObservationWithoutDiscardingLinks() = runBlocking {
        var fail = true
        val failure = IllegalStateException("Unavailable")
        val original = dependencies.resumesRepository
        val repository = object : ResumesRepository by original {
            override fun observeResumes(projectId: Long) = flow {
                if (fail) throw failure
                emitAll(original.observeResumes(projectId))
            }
        }
        val failed = CompletableDeferred<Unit>()
        val loaded = CompletableDeferred<Unit>()
        var links = listOf(VacancyResume(1, 40))
        lateinit var component: DefaultVacancyResumesComponent
        runOnUiThread {
            lifecycle.resume()
            val context = DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val resumesUseCases = ResumesUseCases(repository)
                },
            )
            component = DefaultVacancyResumesComponent(
                context,
                7,
                current = { links },
                changed = { links = it },
                canEdit = { true },
            )
            component.state.subscribe {
                if (it.failed) failed.complete(Unit)
                if (it.available.isNotEmpty()) loaded.complete(Unit)
            }
        }
        withTimeout(5000) { failed.await() }
        val logged = dependencies.logger.failures.receive()
        assertEquals(failure.message, logged.error.message)
        assertEquals("Load vacancy resume choices", logged.operation)
        assertEquals(mapOf("projectId" to 7L), logged.context)
        runOnUiThread {
            fail = false
            original.records.value =
                listOf(Resume(id = 2, projectId = 7, name = "New", createdAt = 0, updatedAt = 0))
            component.onRetry()
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            assertEquals(false, component.state.value.failed)
            component.onSelect(2)
            assertEquals(listOf(VacancyResume(1, 40), VacancyResume(2)), links)
        }
    }
}
