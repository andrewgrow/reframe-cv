package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue

class TestCoverLetterExportComponent(
    private val download: () -> Unit = {
    },
) : CoverLetterExportComponent {
    override val format = MutableValue(CoverLetterExportFormat.Pdf)
    override fun onFormatChange(format: CoverLetterExportFormat) {
        if (state.value != CoverLetterExportState.Exporting) {
            this.format.value = format
            state.value = CoverLetterExportState.Idle
        }
    }
    override val state = MutableValue(CoverLetterExportState.Idle)
    override fun onDownload() = download()
    override fun onReset() {
        if (state.value !=
            CoverLetterExportState.Exporting
        ) {
            state.value = CoverLetterExportState.Idle
            format.value = CoverLetterExportFormat.Pdf
        }
    }
}
