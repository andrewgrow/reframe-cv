package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue

class TestVacancyExportComponent(private val download: () -> Unit = {}) : VacancyExportComponent {
    override val state = MutableValue(VacancyExportState.Idle)
    override val format = MutableValue(VacancyExportFormat.Pdf)
    override val archive = MutableValue(VacancyArchiveDraft())
    override fun onArchiveName(name: String) {
        archive.value = archive.value.copy(
            name = name,
            invalidName = reframecv.export.archiveFilePrefix(name).isBlank(),
        )
    }
    override fun onResumeToggle(id: Long) {
        val ids = archive.value.resumeIds
        archive.value = archive.value.copy(resumeIds = if (id in ids) ids - id else ids + id)
    }
    override fun onLetterToggle(id: Long) {
        val ids = archive.value.letterIds
        archive.value = archive.value.copy(letterIds = if (id in ids) ids - id else ids + id)
    }
    override fun onRetryArchive() = Unit
    override fun onFormatChange(format: VacancyExportFormat) {
        if (state.value != VacancyExportState.Exporting) this.format.value = format
    }
    override fun onDownload() = download()
    override fun onReset() {
        if (state.value != VacancyExportState.Exporting) {
            state.value = VacancyExportState.Idle
            format.value = VacancyExportFormat.Pdf
            archive.value = VacancyArchiveDraft()
        }
    }
}
