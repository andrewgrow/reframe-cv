package reframecv

import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import io.klogging.config.ANSI_INFO
import io.klogging.config.loggingConfiguration
import io.klogging.noCoLogger
import kotlinx.coroutines.runBlocking
import reframecv.dependencies.DefaultApplicationDependencies
import reframecv.logging.ApplicationLogger
import reframecv.repository.AppSettingsRepository
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.application.RootComponent
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.window.DesktopApplicationWindow
import reframecv.window.DesktopWindowSettingsStore

// default JVM entry point
fun main() {
    loggingConfiguration { ANSI_INFO() }
    noCoLogger("App").info("Application started")

    val dependencies = DefaultApplicationDependencies()
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
        dependencies.logger.error(
            "Uncaught application error",
            error,
            mapOf("thread" to thread.name),
        )
        if (previousHandler != null) {
            previousHandler.uncaughtException(thread, error)
        } else {
            error.printStackTrace()
        }
    }
    val lifecycle = LifecycleRegistry()
    try {
        val rootComponent = runOnUiThread {
            val context =
                DefaultAppComponentContext(DefaultComponentContext(lifecycle), dependencies)
            DefaultRootComponent(context)
        }
        runApplication(
            rootComponent,
            lifecycle,
            dependencies.appSettingsRepository,
            dependencies.logger,
        )
    } finally {
        runOnUiThread { lifecycle.destroy() }
        dependencies.close()
    }
}

// Starts the shared application root and hosts its Compose content.
fun runApplication(
    rootComponent: RootComponent,
    lifecycle: LifecycleRegistry,
    settingsRepository: AppSettingsRepository,
    logger: ApplicationLogger = ApplicationLogger.None,
) {
    val settings = runBlocking { DesktopWindowSettingsStore.load(settingsRepository, logger) }
    try {
        application {
            DesktopApplicationWindow(rootComponent, lifecycle, settings, ::exitApplication)
        }
    } finally {
        runBlocking { settings.save() }
    }
}
