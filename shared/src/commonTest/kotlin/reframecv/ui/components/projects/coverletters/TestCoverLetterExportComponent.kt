package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue

class TestCoverLetterExportComponent(
    private val download: () -> Unit = {
    },
) : CoverLetterExportComponent {
    override val state = MutableValue(CoverLetterExportState.Idle)
    override fun onDownload() = download()
    override fun onReset() {
        if (state.value !=
            CoverLetterExportState.Exporting
        ) {
            state.value = CoverLetterExportState.Idle
        }
    }
}
