package reframecv.ui.components.projects.coverletters

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.VacanciesComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent
import reframecv.ui.threading.runOnUiThread

class CoverLettersNavigationTest : ComponentTest() {
    @Test
    fun opensLinkedVacancyAndBackRetainsSelectedLettersThenLetterEditorReturnsToList() =
        runBlocking<Unit> {
            val letter = CoverLetter(2, 7, "Letter", 0, 0, sourceResumeId = 1)
            dependencies.coverLettersRepository.records.value = listOf(letter)
            dependencies.vacanciesRepository.records.value =
                listOf(Vacancy(1, 7, "Main", 0, 0, coverLetterIds = listOf(2)))
            val ready = CompletableDeferred<Unit>()
            val opened = CompletableDeferred<VacancyEditorComponent>()
            val component = runOnUiThread {
                lifecycle.resume()
                DefaultCoverLettersComponent(appComponentContext(), 7, back = {
                    error("Must return to letter list")
                }).also { host ->
                    host.uiState.subscribe {
                        if (it is CoverLettersState.Ready &&
                            it.linkedVacancies.isNotEmpty()
                        ) {
                            ready.complete(Unit)
                        }
                    }
                    host.childStack.subscribe { stack ->
                        val source =
                            stack.active.instance as? CoverLettersComponent.Child.LinkedVacancy
                        source?.component?.childStack?.subscribe {
                            val page = it.active.instance as? VacanciesComponent.Child.Editor
                            page?.component?.let { editor -> opened.complete(editor) }
                        }
                    }
                }
            }
            withTimeout(5000) { ready.await() }
            runOnUiThread {
                component.selection.onToggle(2)
                component.onOpenVacancy(999)
                assertIs<CoverLettersComponent.Child.List>(
                    component.childStack.value.active.instance,
                )
                component.onOpenVacancy(1)
            }
            val editor = withTimeout(5000) { opened.await() }
            runOnUiThread {
                assertEquals(1L, editor.initialVacancy!!.id)
                component.onBack()
                assertIs<CoverLettersComponent.Child.List>(
                    component.childStack.value.active.instance,
                )
                assertEquals(setOf(2L), component.selection.state.value.selected)
                component.onEdit(letter)
                val page =
                    assertIs<CoverLettersComponent.Child.Editor>(
                        component.childStack.value.active.instance,
                    )
                assertEquals(2L, page.component.state.value.record.id)
                component.onBack()
                assertIs<CoverLettersComponent.Child.List>(
                    component.childStack.value.active.instance,
                )
            }
        }

    @Test
    fun liveVacancyNamesAndMissingVacanciesAreReflectedWithoutChangingIdOrder() =
        runBlocking<Unit> {
            dependencies.coverLettersRepository.records.value = listOf(
                CoverLetter(3, 7, "A", 0, 0),
                CoverLetter(1, 7, "Z", 0, 0, sourceResumeId = 5),
                CoverLetter(2, 9, "Other project", 0, 0),
            )
            val source = Vacancy(5, 7, "Original", 0, 0, coverLetterIds = listOf(1))
            dependencies.vacanciesRepository.records.value = listOf(source)
            val updates = Channel<CoverLettersState.Ready>(Channel.UNLIMITED)
            runOnUiThread {
                lifecycle.resume()
                DefaultCoverLettersComponent(appComponentContext(), 7, back = {
                }).uiState.subscribe {
                    if (it is CoverLettersState.Ready) updates.trySend(it)
                }
            }
            val first = withTimeout(5000) { updates.receive() }
            assertEquals(listOf(1L, 3L), first.letters.map { it.id })
            assertEquals("Original", first.linkedVacancies.single().name)
            dependencies.vacanciesRepository.records.value = listOf(source.copy(name = "Renamed"))
            val renamed = withTimeout(5000) {
                var update = updates.receive()
                while (update.linkedVacancies.singleOrNull()?.name != "Renamed") {
                    update = updates.receive()
                }
                update
            }
            assertEquals("Renamed", renamed.linkedVacancies.single().name)
            dependencies.vacanciesRepository.records.value = emptyList()
            val removed = withTimeout(5000) {
                var update = updates.receive()
                while (update.linkedVacancies.isNotEmpty()) update = updates.receive()
                update
            }
            assertEquals(emptyList(), removed.linkedVacancies)
            updates.close()
        }
}
