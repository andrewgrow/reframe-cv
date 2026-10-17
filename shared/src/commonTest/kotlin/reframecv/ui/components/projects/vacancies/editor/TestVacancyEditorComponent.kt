package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.vacancy.Vacancy

class TestVacancyEditorComponent(
    override val initialVacancy: Vacancy? = null,
    private val save: (VacancyDraft) -> Unit = {},
    private val close: () -> Unit = {},
    private val delete: () -> Unit = {},
    imported: () -> Unit = {},
) : VacancyEditorComponent {
    override val draft =
        MutableValue(initialVacancy?.let(VacancyDraft::fromVacancy) ?: VacancyDraft(""))
    override fun onDraftChange(draft: VacancyDraft) {
        this.draft.value = draft
    }
    override val importComponent = TestVacancyImportComponent(imported)
    override fun onImport() {
        if (initialVacancy == null && !isBusy) importComponent.onImport()
    }
    override val recognition = TestVacancyRecognitionComponent()
    override fun onRecognize() {
        if (!isBusy) recognition.onRecognize()
    }
    override val snapshot = TestVacancySnapshotComponent()
    override val exportVisible = MutableValue(false)
    override val exportComponent = TestVacancyExportComponent { onCloseExport() }
    override fun onExport() {
        if (initialVacancy != null && !isBusy && draft.value.name.isNotBlank()) {
            save(draft.value)
            exportVisible.value = true
        }
    }
    override fun onCloseExport() {
        if (!isBusy) {
            exportVisible.value = false
            exportComponent.onReset()
        }
    }
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
        if (recognition.state.value == VacancyRecognitionState.Processing) {
            recognition.onCancel()
            return
        }
        if (isBusy) {
            return
        }
        if (exportVisible.value) {
            onCloseExport()
        } else if (deleteState.value !=
            VacancyDeleteState.Idle
        ) {
            deleteState.value = VacancyDeleteState.Idle
        } else {
            close()
        }
    }
}
