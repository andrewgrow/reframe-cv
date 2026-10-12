package reframecv.ui.compose.common

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun documentDate(timestamp: Long, timeZone: TimeZone): String =
    Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timeZone).date.toString()
