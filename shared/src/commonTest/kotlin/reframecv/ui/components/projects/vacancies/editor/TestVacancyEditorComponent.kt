package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.vacancy.Vacancy

class TestVacancyEditorComponent(
    override val initialVacancy: Vacancy? = null,
    private val save: (VacancyDraft) -> Unit = {},
    private val close: () -> Unit = {},
    private val delete: () -> Unit = {},
) : VacancyEditorComponent {
    override val draft =
        MutableValue(initialVacancy?.let(VacancyDraft::fromVacancy) ?: VacancyDraft(""))
    override fun onDraftChange(draft: VacancyDraft) {
        this.draft.value = draft
    }
    override val snapshot = TestVacancySnapshotComponent()
    override val deleteState = MutableValue(VacancyDeleteState.Idle)
    override val saveState = MutableValue(VacancySaveState.Idle)
    override fun onSave(draft: VacancyDraft) = save(draft)
    override fun onDelete() {
        if (deleteState.value ==
            VacancyDeleteState.Idle
        ) {
            deleteState.value = VacancyDeleteState.Confirming
        } else {
            delete()
        }
    }
    override fun onClose() {
        if (saveState.value == VacancySaveState.Saving ||
            deleteState.value == VacancyDeleteState.Deleting
        ) {
            return
        }
        if (deleteState.value !=
            VacancyDeleteState.Idle
        ) {
            deleteState.value = VacancyDeleteState.Idle
        } else {
            close()
        }
    }
}
