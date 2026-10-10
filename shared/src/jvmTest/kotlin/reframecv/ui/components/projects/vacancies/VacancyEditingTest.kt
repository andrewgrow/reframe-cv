package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.repository.VacanciesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class VacancyEditingTest : ComponentTest() {
    private fun original() = Vacancy(
        id = 12, projectId = 7, name = "Old title", company = "Old company",
        description = "Old description", url = "old url", keywords = listOf("Old"),
        createdAt = 100, updatedAt = 200, resumeId = 3, coverLetterId = 4, importRecordId = 5,
    )

    @Test
    fun updatesExistingRecordAndPreservesIdentityTimestampsAndLinks() = runBlocking {
        val original = original()
        val another = original.copy(id = 13, name = "Another")
        dependencies.vacanciesRepository.records.value = listOf(original, another)
        val closed = CompletableDeferred<Unit>()
        lateinit var component: DefaultVacanciesComponent
        val draft =
            VacancyDraft(
                " New title ",
                " New company ",
                " New description ",
                " new url ",
                " Kotlin, Android ",
            )
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacanciesComponent(appComponentContext(), 7, back = {})
            component.onEdit(original)
            val editor = requireNotNull(component.editorSlot.value.child).instance
            assertEquals(original, editor.initialVacancy)
            component.editorSlot.subscribe { if (it.child == null) closed.complete(Unit) }
            editor.onSave(draft)
        }
        withTimeout(10_000.milliseconds) { closed.await() }
        runOnUiThread {
            assertNull(component.editorSlot.value.child)
            assertEquals(
                listOf(draft.applyTo(original), another),
                dependencies.vacanciesRepository.records.value,
            )
        }
    }

    @Test
    fun failedUpdateStaysOpenAndCanBeRetried() = runBlocking {
        val original = original()
        dependencies.vacanciesRepository.records.value = listOf(original)
        var fail = true
        val repository = object : VacanciesRepository by dependencies.vacanciesRepository {
            override suspend fun update(record: Vacancy) {
                if (fail) error("Update failed")
                dependencies.vacanciesRepository.update(record)
            }
        }
        val states = Channel<VacancySaveState>(Channel.UNLIMITED)
        lateinit var component: DefaultVacanciesComponent
        val draft = VacancyDraft("New title")
        runOnUiThread {
            lifecycle.resume()
            val context = DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val vacanciesRepository = repository
                },
            )
            component = DefaultVacanciesComponent(context, 7, back = {})
            component.onEdit(original)
            val editor = requireNotNull(component.editorSlot.value.child).instance
            editor.saveState.subscribe { states.trySend(it) }
            editor.onSave(draft)
        }
        withTimeout(10_000.milliseconds) {
            while (states.receive() != VacancySaveState.Failed) { /* Wait for update failure. */ }
            assertEquals(listOf(original), dependencies.vacanciesRepository.records.value)
            val closed = CompletableDeferred<Unit>()
            runOnUiThread {
                fail = false
                component.editorSlot.subscribe { if (it.child == null) closed.complete(Unit) }
                requireNotNull(component.editorSlot.value.child).instance.onSave(draft)
            }
            closed.await()
        }
        assertEquals(
            listOf(draft.applyTo(original)),
            dependencies.vacanciesRepository.records.value,
        )
    }

    @Test
    fun cancellationPreservesRecordAndRejectsDifferentProject() = runOnUiThread {
        lifecycle.resume()
        val component = DefaultVacanciesComponent(appComponentContext(), 7, back = {})
        val original = original()
        dependencies.vacanciesRepository.records.value = listOf(original)
        component.onEdit(original.copy(projectId = 8))
        assertNull(component.editorSlot.value.child)
        component.onEdit(original)
        requireNotNull(component.editorSlot.value.child).instance.onClose()
        assertNull(component.editorSlot.value.child)
        assertEquals(listOf(original), dependencies.vacanciesRepository.records.value)
    }
}
