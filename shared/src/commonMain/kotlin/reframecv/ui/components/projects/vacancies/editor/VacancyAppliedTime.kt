package reframecv.ui.components.projects.vacancies.editor

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import reframecv.shared.nowMillis

private val appliedFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}
private val appliedPattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}")

internal fun appliedTimeText(
    timestamp: Long = nowMillis(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
) = appliedFormat.format(Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(zone))

internal fun parseAppliedTime(
    text: String,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Long? {
    if (!appliedPattern.matches(text)) return null
    return try {
        val local = LocalDateTime.parse(text, appliedFormat)
        val instant = local.toInstant(zone)
        instant.toEpochMilliseconds().takeIf { instant.toLocalDateTime(zone) == local }
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun VacancyDraft.validAppliedTime() = !applied || parseAppliedTime(appliedDate) != null
