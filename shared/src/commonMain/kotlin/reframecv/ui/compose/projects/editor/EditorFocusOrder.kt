package reframecv.ui.compose.projects.editor

import androidx.compose.ui.focus.FocusRequester

internal class EditorFocusOrder {
    val name = FocusRequester()
    val update = FocusRequester()
    val cancel = FocusRequester()
    val delete = FocusRequester()
}
