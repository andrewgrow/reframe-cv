package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.context.AppComponentContext

/** Lets the vacancy editor show save progress and failures. */
enum class VacancySaveState { Idle, Saving, Failed }

/** Controls inline deletion confirmation, progress, and failure feedback. */
enum class VacancyDeleteState { Idle, Confirming, Deleting, Failed }

/** Lets the vacancy page create, edit, or confirm deletion of a vacancy. */
interface VacancyEditorComponent {
    val draft: Value<VacancyDraft>
    fun onDraftChange(draft: VacancyDraft)
    val snapshot: VacancySnapshotComponent
    val initialVacancy: Vacancy?
    val deleteState: Value<VacancyDeleteState>
    val saveState: Value<VacancySaveState>
    val exportVisible: Value<Boolean>
    val exportComponent: VacancyExportComponent
    fun onExport()
    fun onCloseExport()
    val isBusy: Boolean
        get() = saveState.value == VacancySaveState.Saving ||
            deleteState.value == VacancyDeleteState.Deleting ||
            exportComponent.state.value == VacancyExportState.Exporting
    fun onSave(draft: VacancyDraft)
    fun onDelete()
    fun onClose()
}

/** Prevents untitled vacancies, accidental deletion, and dismissal during unfinished changes. */
class DefaultVacancyEditorComponent(
    componentContext: AppComponentContext,
    private val save: (VacancyDraft) -> Unit,
    private val close: () -> Unit,
    override val initialVacancy: Vacancy? = null,
    private val delete: () -> Unit = {},
) : VacancyEditorComponent,
    AppComponentContext by componentContext {
    override val draft =
        MutableValue(initialVacancy?.let(VacancyDraft::fromVacancy) ?: VacancyDraft(""))
    override val snapshot = DefaultVacancySnapshotComponent(
        componentContext,
        initialVacancy?.id,
        initialVacancy?.vacancyAttachmentId != null,
    )
    override val saveState = MutableValue(VacancySaveState.Idle)
    override val exportVisible = MutableValue(false)
    override val exportComponent = DefaultVacancyExportComponent(
        componentContext,
        vacancy = {
            initialVacancy?.takeIf { exportVisible.value }?.let { draft.value.applyTo(it) }
        },
        finished = ::onCloseExport,
    )
    private var exportAfterSave = false
    override val deleteState = MutableValue(VacancyDeleteState.Idle)
    override fun onDraftChange(draft: VacancyDraft) {
        if (!isBusy && !exportVisible.value && deleteState.value == VacancyDeleteState.Idle) {
            this.draft.value = draft
        }
    }

    override fun onSave(draft: VacancyDraft) = requestSave(draft, export = false)

    override fun onExport() {
        if (initialVacancy != null) requestSave(draft.value, export = true)
    }

    private fun requestSave(draft: VacancyDraft, export: Boolean) {
        if (isBusy || exportVisible.value || snapshot.state.value.selecting) return
        if (draft.title.isNotBlank() && deleteState.value == VacancyDeleteState.Idle) {
            exportAfterSave = export
            saveState.value = VacancySaveState.Saving
            save(draft.copy(snapshotChange = snapshot.change))
        }
    }

    internal fun onSaveCompleted(state: VacancySaveState): Boolean {
        saveState.value = state
        if (state == VacancySaveState.Idle) {
            snapshot.onSaved()
            exportVisible.value = exportAfterSave
        }
        exportAfterSave = false
        return exportVisible.value
    }

    override fun onCloseExport() {
        if (!isBusy) {
            exportVisible.value = false
            exportComponent.onReset()
        }
    }

    override fun onDelete() {
        if (initialVacancy != null && !isBusy && !exportVisible.value) {
            if (deleteState.value == VacancyDeleteState.Idle) {
                deleteState.value = VacancyDeleteState.Confirming
            } else {
                delete()
            }
        }
    }

    override fun onClose() {
        if (!isBusy) {
            if (exportVisible.value) {
                onCloseExport()
            } else if (deleteState.value != VacancyDeleteState.Idle) {
                deleteState.value = VacancyDeleteState.Idle
            } else {
                close()
            }
        }
    }
}
