package reframecv.ui.components.common.confirmation

import org.jetbrains.compose.resources.StringResource

/** Supplies localized copy and appearance for a reusable confirmation dialog. */
data class ConfirmationDialogContent(
    val title: StringResource,
    val message: StringResource,
    val confirmLabel: StringResource,
    val cancelLabel: StringResource,
    val subject: String? = null,
    val inputLabel: StringResource? = null,
    val failureMessage: StringResource? = null,
    val tone: ConfirmationDialogTone = ConfirmationDialogTone.Standard,
)

/** Distinguishes routine confirmations from destructive operations. */
enum class ConfirmationDialogTone { Standard, Critical }
