package reframecv.shared

import kotlin.time.Clock

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
