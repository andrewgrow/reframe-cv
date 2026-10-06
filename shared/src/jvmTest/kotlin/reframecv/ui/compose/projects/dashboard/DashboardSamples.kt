package reframecv.ui.compose.projects.dashboard

import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.dashboard.DashboardState

internal fun dashboardSample() = DashboardState.Ready(
    resumes = listOf(
        Resume(id = 1, projectId = 1, name = "Android Developer", createdAt = 1, updatedAt = 1),
        Resume(
            id = 2,
            projectId = 1,
            name = "Senior Kotlin Multiplatform Developer",
            createdAt = 1,
            updatedAt = 1,
        ),
        Resume(id = 3, projectId = 1, name = "Backend Developer", createdAt = 1, updatedAt = 1),
    ),
    vacancies = listOf(
        Vacancy(
            id = 1,
            projectId = 1,
            name = "Mobile Engineer at Example",
            createdAt = 1,
            updatedAt = 1,
        ),
        Vacancy(id = 2, projectId = 1, name = "Kotlin Developer", createdAt = 1, updatedAt = 1),
    ),
    coverLetters = listOf(
        CoverLetter(
            id = 1,
            projectId = 1,
            name = "Introduction to the mobile team",
            createdAt = 1,
            updatedAt = 1,
        ),
    ),
)
