package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyEditorComponent

class TestVacanciesComponent(
    override val projectId: Long = 1,
    initialState: VacanciesState = VacanciesState.Ready(emptyList()),
    private val retry: () -> Unit = {},
    private val back: () -> Unit = {},
) : VacanciesComponent {
    override val uiState = MutableValue(initialState)
    override val editorSlot =
        MutableValue<ChildSlot<*, VacancyEditorComponent>>(
            ChildSlot<Unit, VacancyEditorComponent>(),
        )
    override fun onAdd() = openEditor(null)
    override fun onEdit(vacancy: Vacancy) = openEditor(vacancy)

    private fun openEditor(vacancy: Vacancy?) {
        val editor = TestVacancyEditorComponent(initialVacancy = vacancy, close = {
            editorSlot.value = ChildSlot<Unit, VacancyEditorComponent>()
        })
        editorSlot.value = ChildSlot(child = Child.Created(Unit, editor))
    }
    override fun onRetry() = retry()
    override fun onBack() = back()
}
