package reframecv.ui.compose.projects.coverletters

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.coverletters.CoverLettersState

internal fun letterListSamples() = CoverLettersState.Ready(
    listOf(
        CoverLetter(
            1,
            1,
            "Mobile platform — cover letter",
            1700000000000,
            1700000000000,
            sourceResumeId = 10,
        ),
        CoverLetter(2, 1, "Backend developer — cover letter", 1700000000000, 1700000000000),
        CoverLetter(
            3,
            1,
            "A longer cover letter title for the product engineering team",
            1700000000000,
            1700000000000,
            sourceResumeId = 11,
        ),
    ),
    listOf(
        Vacancy(10, 1, "Mobile developer", 0, 0, coverLetterIds = listOf(1)),
        Vacancy(11, 1, "Backend developer", 0, 0, coverLetterIds = listOf(3)),
    ),
)
