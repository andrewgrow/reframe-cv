package reframecv.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import reframecv.ui.components.application.RootComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme

private const val SETTINGS_SAVE_DELAY_MILLIS = 500L

@OptIn(FlowPreview::class)
@Composable
internal fun DesktopApplicationWindow(
    rootComponent: RootComponent,
    lifecycle: LifecycleRegistry,
    settings: DesktopWindowSettingsStore,
    onClose: () -> Unit,
) {
    val initial = remember(settings) {
        val screen = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        val configuration = screen.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
        settings.current.fitToScreen(
            (configuration.bounds.width - insets.left - insets.right).toFloat(),
            (configuration.bounds.height - insets.top - insets.bottom).toFloat(),
        )
    }
    val state = rememberWindowState(
        size = initial.size,
        placement = if (initial.maximized) WindowPlacement.Maximized else WindowPlacement.Floating,
    )
    fun capture() = settings.current.capture(state.size, state.placement, state.isMinimized)
    LifecycleController(lifecycle, state)
    LaunchedEffect(state, settings) {
        snapshotFlow { capture() }
            .onEach(settings::update)
            .debounce(SETTINGS_SAVE_DELAY_MILLIS.milliseconds)
            .collect { settings.save() }
    }
    Window(
        onCloseRequest = {
            settings.update(capture())
            runBlocking { settings.save() }
            onClose()
        },
        state = state,
        title = "ReframeCV",
    ) {
        ReframeTheme { RootContent(rootComponent) }
    }
}
