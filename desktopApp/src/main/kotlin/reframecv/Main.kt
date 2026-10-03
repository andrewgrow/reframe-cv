package reframecv

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import io.klogging.config.ANSI_INFO
import io.klogging.config.loggingConfiguration
import io.klogging.noCoLogger
import reframecv.dependencies.DefaultApplicationDependencies
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.application.RootComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.threading.runOnUiThread

// default JVM entry point
fun main() {
    loggingConfiguration { ANSI_INFO() }
    noCoLogger("App").info("Application started")

    val dependencies = DefaultApplicationDependencies()
    val lifecycle = LifecycleRegistry()
    try {
        val rootComponent = runOnUiThread {
            val context =
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), dependencies)
            DefaultRootComponent(context)
        }
        runApplication(rootComponent, lifecycle)
    } finally {
        runOnUiThread { lifecycle.destroy() }
        dependencies.close()
    }
}

// Starts the shared application root and hosts its Compose content.
fun runApplication(rootComponent: RootComponent, lifecycle: LifecycleRegistry) = application {
    val windowState = rememberWindowState()

    LifecycleController(lifecycle, windowState)

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "ReframeCV",
    ) {
        ReframeTheme {
            RootContent(rootComponent)
        }
    }
}
