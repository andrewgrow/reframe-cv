package reframecv.ui.compose.projects.vacancies

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun vacancyDate(timestamp: Long, timeZone: TimeZone): String =
    Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date.toString()
