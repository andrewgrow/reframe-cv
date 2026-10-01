package reframecv

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.klogging.config.ANSI_INFO
import io.klogging.config.loggingConfiguration
import io.klogging.noCoLogger
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.application.RootComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.threading.runOnUiThread

// default JVM entry point
fun main() {
    loggingConfiguration { ANSI_INFO() }
    noCoLogger("App").info("Application started")

    val lifecycle = LifecycleRegistry()
    val context = DefaultComponentContext(lifecycle = lifecycle)
    val rootComponent = runOnUiThread {
        DefaultRootComponent(componentContext = context)
    }
    runApplication(rootComponent, lifecycle)
}

// Starts the shared application root and hosts its Compose content.
fun runApplication(rootComponent: RootComponent, lifecycle: LifecycleRegistry) = application {
    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Reframe CV",
    ) {
        RootContent(rootComponent)
    }
}
