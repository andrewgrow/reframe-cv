package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.coverletter.CoverLetter

class TestCoverLettersComponent(
    override val projectId: Long = 1,
    initialState: CoverLettersState = CoverLettersState.Ready(emptyList()),
    private val edit: (CoverLetter) -> Unit = {},
    private val openResume: (Long) -> Unit = {},
    private val retry: () -> Unit = {},
    private val back: () -> Unit = {},
) : CoverLettersComponent {
    override val uiState = MutableValue(initialState)
    override val selection = TestCoverLetterSelectionComponent { uiState.value }
    override val childStack = MutableValue(
        ChildStack<Any, CoverLettersComponent.Child>(
            active = Child.Created("list", CoverLettersComponent.Child.List),
            backStack = emptyList(),
        ),
    )
    override fun onAdd() = Unit
    override fun onEdit(letter: CoverLetter) = edit(letter)
    override fun onOpenResume(id: Long) = openResume(id)
    override fun onRetry() = retry()
    override fun onBack() = back()
}
