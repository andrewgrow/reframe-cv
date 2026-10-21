package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue

class TestCoverLetterImportComponent(
    private val imported: () -> Unit = {
    },
) : CoverLetterImportComponent {
    override val state = MutableValue(CoverLetterImportState.Idle)
    override fun onImport() = imported()
    override fun onReset() {
        state.value = CoverLetterImportState.Idle
    }
    override fun onClose() = Unit
}
