package reframecv.ui.compose.common

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
internal fun Modifier.pageBackHandler(
    onBack: () -> Unit,
    enabled: Boolean = true,
    destination: Any? = Unit,
): Modifier {
    val focus = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(destination, enabled, windowFocused, hasFocus) {
        // Hover clears control focus; retain a page target for keyboard Back without stealing it.
        if (enabled && windowFocused && !hasFocus) focus.requestFocus()
    }
    return onPreviewKeyEvent { event ->
        if (enabled && event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
            onBack()
            true
        } else {
            false
        }
    }.onFocusChanged { hasFocus = it.hasFocus }.focusRequester(focus).focusable(enabled)
}
