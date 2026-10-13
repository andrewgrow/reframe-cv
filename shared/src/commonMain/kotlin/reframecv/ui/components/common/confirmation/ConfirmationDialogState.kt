package reframecv.ui.components.common.confirmation

import org.jetbrains.compose.resources.StringResource

/** Supplies component-owned input, validation, progress, and failure feedback. */
data class ConfirmationDialogState(
    val input: String = "",
    val confirmEnabled: Boolean = true,
    val busy: Boolean = false,
    val error: StringResource? = null,
)
