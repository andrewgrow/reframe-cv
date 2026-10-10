package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.context.AppComponentContext

enum class VacancySaveState { Idle, Saving, Failed }
enum class VacancyDeleteState { Idle, Confirming, Deleting, Failed }

interface VacancyEditorComponent {
    val initialVacancy: Vacancy?
    val deleteState: Value<VacancyDeleteState>
    val saveState: Value<VacancySaveState>
    fun onSave(draft: VacancyDraft)
    fun onDelete()
    fun onClose()
}

class DefaultVacancyEditorComponent(
    componentContext: AppComponentContext,
    private val save: (VacancyDraft) -> Unit,
    private val close: () -> Unit,
    override val initialVacancy: Vacancy? = null,
    private val delete: () -> Unit = {},
) : VacancyEditorComponent,
    AppComponentContext by componentContext {
    override val saveState = MutableValue(VacancySaveState.Idle)
    override val deleteState = MutableValue(VacancyDeleteState.Idle)
    private val isBusy: Boolean
        get() = saveState.value == VacancySaveState.Saving ||
            deleteState.value == VacancyDeleteState.Deleting

    override fun onSave(draft: VacancyDraft) {
        if (draft.title.isNotBlank() && !isBusy &&
            deleteState.value == VacancyDeleteState.Idle
        ) {
            save(draft)
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
