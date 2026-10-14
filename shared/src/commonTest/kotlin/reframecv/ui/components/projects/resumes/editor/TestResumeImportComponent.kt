package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue

class TestResumeImportComponent(private val import: () -> Unit = {}) : ResumeImportComponent {
    override val state = MutableValue(ResumeImportState.Idle)
    override fun onImport() = import()
    override fun onReset() {
        if (state.value != ResumeImportState.Importing) state.value = ResumeImportState.Idle
    }
    override fun onClose() = Unit
}
