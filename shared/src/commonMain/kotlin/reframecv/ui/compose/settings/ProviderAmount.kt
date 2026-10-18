package reframecv.ui.compose.settings

import kotlin.math.abs
import kotlin.math.roundToLong

private const val CENTS_PER_DOLLAR = 100

/** Displays provider amounts in cents while retaining their original precision in storage. */
internal fun providerAmount(value: Double): String {
    val cents = (abs(value) * CENTS_PER_DOLLAR).roundToLong()
    val sign = if (value < 0 && cents != 0L) "-" else ""
    return "$sign${cents / CENTS_PER_DOLLAR}.${(cents % CENTS_PER_DOLLAR).toString().padStart(
        2,
        '0',
    )}"
}
