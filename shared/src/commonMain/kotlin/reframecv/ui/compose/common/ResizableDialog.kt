package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resize_dialog
import reframecv.ui.theme.ReframeTheme

/** Provides a reusable dialog with user-controlled size and fixed title/actions. */
@Composable
internal fun ResizableDialog(
    onDismissRequest: () -> Unit,
    resizingEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    val state = rememberSaveable(saver = DialogResizeState.saver) { DialogResizeState() }
    val density = LocalDensity.current.density
    val window = LocalWindowInfo.current.containerSize
    val dimensions = ReframeTheme.tokens.dimensions
    val size = if (state.resized) Modifier.size(state.width.dp, state.height.dp) else Modifier
    Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier.then(
                if (state.resized) {
                    size
                } else {
                    Modifier.widthIn(
                        dimensions.dialogMinWidth,
                        dimensions.dialogDefaultWidth,
                    )
                },
            ).pageBackHandler(onDismissRequest, destination = resizingEnabled)
                .onSizeChanged { state.captureMinimum(it, density) },
        ) {
            Surface(
                modifier = if (state.resized) Modifier.fillMaxSize() else Modifier,
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                ResizableDialogLayout(state.resized, title, text, actions)
            }
            ResizeHandle(
                label = stringResource(Res.string.resize_dialog),
                enabled = resizingEnabled,
                onDrag = { delta, _ -> state.resize(delta, density, window) },
                modifier = Modifier.align(Alignment.BottomEnd).testTag("dialog.resize"),
            )
        }
    }
}

@Composable
private fun ResizableDialogLayout(
    resized: Boolean,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    actions: @Composable () -> Unit,
) {
    val tokens = ReframeTheme.tokens
    Column(Modifier.padding(tokens.spacing.large)) {
        CompositionLocalProvider(
            LocalContentColor provides AlertDialogDefaults.titleContentColor,
            LocalTextStyle provides tokens.typography.headlineSmall,
        ) {
            Box(Modifier.padding(bottom = tokens.spacing.medium)) { title() }
        }
        CompositionLocalProvider(
            LocalContentColor provides AlertDialogDefaults.textContentColor,
            LocalTextStyle provides tokens.typography.bodyMedium,
        ) {
            Box(Modifier.weight(1f, fill = resized).padding(bottom = tokens.spacing.large)) {
                text()
            }
        }
        CompositionLocalProvider(LocalTextStyle provides tokens.typography.labelLarge) {
            Box(Modifier.align(Alignment.End)) { actions() }
        }
    }
}
