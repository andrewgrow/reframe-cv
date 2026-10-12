package reframecv.ui.compose.projects.resumes

import kotlin.time.Instant
import reframecv.domain.models.resume.Resume

internal fun resumeSamples() = listOf(
    "Android Developer",
    "Kotlin Multiplatform Engineer — resume for desktop and mobile development",
    "Backend Developer",
).mapIndexed { index, name ->
    Resume(
        id = index.toLong() + 1,
        projectId = 1,
        name = name,
        createdAt = Instant.parse("2026-10-10T12:00:00Z").toEpochMilliseconds(),
        updatedAt = 0,
    )
}
