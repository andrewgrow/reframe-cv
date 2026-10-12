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
    val isBusy: Boolean
        get() = saveState.value == VacancySaveState.Saving ||
            deleteState.value == VacancyDeleteState.Deleting
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
        initialVacancy?.importRecordId != null,
    )
    override val saveState = MutableValue(VacancySaveState.Idle)
    override val deleteState = MutableValue(VacancyDeleteState.Idle)
    override fun onDraftChange(draft: VacancyDraft) {
        if (!isBusy && deleteState.value == VacancyDeleteState.Idle) this.draft.value = draft
    }

    override fun onSave(draft: VacancyDraft) {
        if (isBusy || snapshot.state.value.selecting) return
        if (draft.title.isNotBlank() && deleteState.value == VacancyDeleteState.Idle) {
            save(draft.copy(snapshotChange = snapshot.change))
        }
    }

    override fun onDelete() {
        if (initialVacancy != null && !isBusy) {
            if (deleteState.value == VacancyDeleteState.Idle) {
                deleteState.value = VacancyDeleteState.Confirming
            } else {
                delete()
            }
        }
    }

    override fun onClose() {
        if (!isBusy) {
            if (deleteState.value != VacancyDeleteState.Idle) {
                deleteState.value = VacancyDeleteState.Idle
            } else {
                close()
            }
        }
    }
}
