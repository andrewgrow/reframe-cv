package reframecv.ui.compose.projects.vacancies

import kotlin.time.Instant
import reframecv.domain.models.vacancy.Vacancy

internal fun vacancySamples() = listOf(
    Vacancy(
        id = 1,
        projectId = 1,
        name = "Senior Android Developer",
        company = "Example",
        createdAt = Instant.parse("2026-10-10T12:00:00Z").toEpochMilliseconds(),
        updatedAt = 0,
    ),
    Vacancy(
        id = 2,
        projectId = 1,
        name = "Kotlin Multiplatform Engineer for desktop and mobile applications",
        company = "A company with a longer name",
        createdAt = Instant.parse("2026-10-09T12:00:00Z").toEpochMilliseconds(),
        updatedAt = 0,
    ),
    Vacancy(
        id = 3,
        projectId = 1,
        name = "Backend Developer",
        createdAt = Instant.parse("2026-10-08T12:00:00Z").toEpochMilliseconds(),
        updatedAt = 0,
    ),
)
