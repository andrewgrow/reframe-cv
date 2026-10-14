package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue

class TestResumeExportComponent(private val download: () -> Unit = {}) : ResumeExportComponent {
    override val state = MutableValue(ResumeExportState.Idle)
    override fun onDownload() = download()
    override fun onReset() {
        if (state.value != ResumeExportState.Exporting) state.value = ResumeExportState.Idle
    }
}
