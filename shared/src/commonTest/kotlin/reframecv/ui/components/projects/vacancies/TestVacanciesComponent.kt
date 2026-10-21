package reframecv.ui.components.projects.vacancies

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent

class TestVacanciesComponent(
    override val projectId: Long = 1,
    initialState: VacanciesState = VacanciesState.Ready(emptyList()),
    private val retry: () -> Unit = {},
    private val back: () -> Unit = {},
) : VacanciesComponent {
    override val uiState = MutableValue(initialState)
    override val selection = TestVacancySelectionComponent { uiState.value }
    override val childStack = MutableValue<ChildStack<*, VacanciesComponent.Child>>(
        ChildStack(Unit, VacanciesComponent.Child.List),
    )
    override fun onAdd() = openEditor(null)
    override fun onEdit(vacancy: Vacancy) = openEditor(vacancy)

    private fun openEditor(vacancy: Vacancy?) {
        val editor = TestVacancyEditorComponent(initialVacancy = vacancy, close = {
            childStack.value = ChildStack(Unit, VacanciesComponent.Child.List)
        })
        childStack.value = ChildStack(
            active = Child.Created("editor", VacanciesComponent.Child.Editor(editor)),
            backStack = listOf(Child.Created(Unit, VacanciesComponent.Child.List)),
        )
    }
    override fun onRetry() = retry()
    override fun onBack() {
        val current = editor
        if (current == null) back() else current.onClose()
    }
}
