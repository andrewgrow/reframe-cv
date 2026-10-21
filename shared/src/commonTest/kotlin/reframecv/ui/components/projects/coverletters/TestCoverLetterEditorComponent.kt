package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.coverletter.CoverLetter

class TestCoverLetterEditorComponent(record: CoverLetter) : CoverLetterEditorComponent {
    override val importComponent = TestCoverLetterImportComponent()
    override val exportComponent = TestCoverLetterExportComponent()
    override fun onImport() = importComponent.onImport()
    override fun onExport() {
        state.value = state.value.copy(exportVisible = true)
    }
    override fun onCloseExport() {
        state.value = state.value.copy(exportVisible = false)
    }
    override val state = MutableValue(CoverLetterEditorState(record))
    override fun onChange(record: CoverLetter) {
        state.value = state.value.copy(record = record)
    }
    override fun onSave() {
        state.value = state.value.copy(notice = LetterNotice.Saved)
    }
    override fun onDelete() {
        state.value = state.value.copy(confirming = true)
    }
    override fun onCancelDelete() {
        state.value = state.value.copy(confirming = false)
    }
    override fun onClose() = Unit
}
