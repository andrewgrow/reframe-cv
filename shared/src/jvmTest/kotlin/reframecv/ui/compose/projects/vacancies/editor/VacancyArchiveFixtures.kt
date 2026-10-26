package reframecv.ui.compose.projects.vacancies.editor

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.ui.components.projects.vacancies.editor.TestVacancyExportComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyArchiveDraft
import reframecv.ui.components.projects.vacancies.editor.VacancyExportFormat

internal fun archiveExportFixture() = TestVacancyExportComponent().also {
    it.format.value = VacancyExportFormat.Zip
    it.archive.value = VacancyArchiveDraft(
        name = "Example application",
        resumes = listOf(
            Resume(1, 1, "Main resume", 0, 0),
            Resume(2, 1, "Tailored resume for Example Systems", 0, 0),
        ),
        letters = listOf(
            CoverLetter(1, 1, "Short cover letter", 0, 0),
            CoverLetter(2, 1, "Detailed cover letter", 0, 0),
        ),
        resumeIds = setOf(1, 2),
        letterIds = setOf(1, 2),
        loaded = true,
    )
}
