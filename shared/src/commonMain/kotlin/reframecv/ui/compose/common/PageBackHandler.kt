package reframecv.ui.compose.common

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(destination, enabled, windowFocused) {
        if (enabled && windowFocused) focus.requestFocus()
    }
    return onPreviewKeyEvent { event ->
        if (enabled && event.key == Key.Escape && event.type == KeyEventType.KeyDown) {
            onBack()
            true
        } else {
            false
        }
    }.focusRequester(focus).focusable(enabled)
}
