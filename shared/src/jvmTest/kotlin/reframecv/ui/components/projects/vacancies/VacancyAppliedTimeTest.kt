package reframecv.ui.components.projects.vacancies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.components.projects.vacancies.editor.appliedTimeText
import reframecv.ui.components.projects.vacancies.editor.parseAppliedTime
import reframecv.ui.threading.runOnUiThread

class VacancyAppliedTimeTest : ComponentTest() {
    @Test
    fun localMinutePrecisionRoundTripUsesTheSpecifiedTimeZone() {
        val zone = TimeZone.of("Europe/Warsaw")
        val timestamp = Instant.parse("2026-10-20T16:45:00Z").toEpochMilliseconds()
        assertEquals("2026-10-20 18:45", appliedTimeText(timestamp, zone))
        assertEquals(timestamp, parseAppliedTime("2026-10-20 18:45", zone))
        assertEquals("2026-10-20 16:45", appliedTimeText(timestamp, TimeZone.UTC))
    }

    @Test
    fun malformedDatesAndNonexistentLocalTimesAreRejected() {
        val zone = TimeZone.of("Europe/Warsaw")
        listOf(
            "",
            "2026-1-20 18:45",
            "2026-02-29 10:00",
            "2026-10-20 24:01",
            "2026-10-20 12:60",
            "2026-10-20T18:45",
            "2026-10-20 18:45:00",
            "2026-03-29 02:30",
        )
            .forEach { assertNull(parseAppliedTime(it, zone), it) }
        assertTrue(parseAppliedTime("2024-02-29 10:00", zone) != null)
    }

    @Test
    fun saveUsesEnteredTimestampAndUncheckingClearsItOnlyInSubmittedDraft() = runOnUiThread<Unit> {
        val original = Vacancy(id = 1, projectId = 1, name = "Job", createdAt = 1, updatedAt = 1)
        val saved = mutableListOf<VacancyDraft>()
        val editor = DefaultVacancyEditorComponent(appComponentContext(), saved::add, {}, original)
        val entered = editor.draft.value.copy(applied = true, appliedDate = "2026-10-20 18:45")
        editor.onDraftChange(entered)
        assertNull(editor.initialVacancy?.appliedAt)
        assertTrue(saved.isEmpty())
        editor.onSave(entered)
        val record = saved.single().applyTo(original)
        assertEquals(parseAppliedTime(entered.appliedDate), record.appliedAt)
        editor.onSaveCompleted(VacancySaveState.Idle, record)
        val cleared = editor.draft.value.copy(applied = false, appliedDate = "invalid but disabled")
        editor.onDraftChange(cleared)
        assertEquals(record.appliedAt, editor.initialVacancy?.appliedAt)
        editor.onSave(cleared)
        assertNull(saved.last().applyTo(record).appliedAt)
        assertTrue(VacancyDraft.fromVacancy(record).applied)
    }

    @Test
    fun checkedInvalidDateBlocksSaveAndExportUntilCorrectedOrUnchecked() = runOnUiThread<Unit> {
        var saves = 0
        val original = Vacancy(id = 1, projectId = 1, name = "Job", createdAt = 1, updatedAt = 1)
        val editor = DefaultVacancyEditorComponent(appComponentContext(), { saves++ }, {}, original)
        editor.onDraftChange(
            editor.draft.value.copy(applied = true, appliedDate = "2026-02-30 12:00"),
        )
        editor.onSave(editor.draft.value)
        editor.onExport()
        assertEquals(0, saves)
        assertEquals(VacancySaveState.InvalidAppliedAt, editor.saveState.value)
        assertFalse(editor.exportVisible.value)
        editor.onDraftChange(editor.draft.value.copy(applied = false))
        assertEquals(VacancySaveState.Idle, editor.saveState.value)
        editor.onSave(editor.draft.value)
        assertEquals(1, saves)
    }
}
