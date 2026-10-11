package reframecv.window

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings
import reframecv.logging.ApplicationLogger
import reframecv.repository.AppSettingsRepository

class DesktopWindowSettingsTest {
    @Test
    fun settingsLoadFromTheRepositoryAndSaveOnlyChanges() = runBlocking {
        val repository = MemorySettingsRepository()
        val store = DesktopWindowSettingsStore.load(repository)
        assertEquals(WindowSettings(), store.current)
        store.save()
        assertEquals(0, repository.writes)
        store.update(WindowSettings(1000f, 700f, true))
        store.save()
        assertEquals(store.current, DesktopWindowSettingsStore.load(repository).current)
        store.save()
        assertEquals(1, repository.writes)
        store.update(WindowSettings(900f, 650f))
        store.save()
        assertEquals(WindowSettings(900f, 650f), repository.getSettings().window)
    }

    @Test
    fun failedSaveCanBeRetriedWithoutLosingTheLatestWindowSize() = runBlocking {
        val repository = MemorySettingsRepository()
        var loggedError: Throwable? = null
        val logger = ApplicationLogger { operation, error, context ->
            assertEquals("Save window settings", operation)
            assertEquals(mapOf("settings" to WindowSettings(1000f, 700f)), context)
            loggedError = error
        }
        val store = DesktopWindowSettingsStore.load(repository, logger)
        store.update(WindowSettings(1000f, 700f))
        repository.failWrites = true
        store.save()
        assertEquals("Test write failure", assertIs<IllegalStateException>(loggedError).message)
        assertEquals(WindowSettings(), repository.getSettings().window)
        repository.failWrites = false
        store.update(WindowSettings(900f, 650f, true))
        store.save()
        assertEquals(store.current, repository.getSettings().window)
    }

    @Test
    fun overlappingResizeAndCloseSavesPersistTheLatestSize() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var firstWrite = true
        val repository = MemorySettingsRepository {
            if (firstWrite) {
                firstWrite = false
                started.complete(Unit)
                release.await()
            }
        }
        val store = DesktopWindowSettingsStore.load(repository)
        store.update(WindowSettings(1000f, 700f))
        val resizeSave = async { store.save() }
        started.await()
        store.update(WindowSettings(900f, 650f, true))
        val closeSave = async { store.save() }
        release.complete(Unit)
        resizeSave.await()
        closeSave.await()
        assertEquals(store.current, repository.getSettings().window)
    }

    @Test
    fun maximizedAndMinimizedWindowsRetainTheirOrdinarySize() {
        val ordinary = WindowSettings(1000f, 700f)
        val screenSize = DpSize(1920.dp, 1080.dp)
        val maximized = ordinary.capture(screenSize, WindowPlacement.Maximized, minimized = false)
        assertEquals(ordinary.copy(maximized = true), maximized)
        assertEquals(
            maximized,
            maximized.capture(DpSize.Zero, WindowPlacement.Floating, minimized = true),
        )
        assertEquals(
            maximized,
            maximized.capture(screenSize, WindowPlacement.Fullscreen, minimized = false),
        )
        assertEquals(
            WindowSettings(950f, 680f),
            maximized.capture(DpSize(950.dp, 680.dp), WindowPlacement.Floating, minimized = false),
        )
        assertEquals(
            ordinary,
            ordinary.capture(DpSize.Unspecified, WindowPlacement.Floating, minimized = false),
        )
    }

    @Test
    fun restorationFitsTheAvailableScreenWithoutEnlargingSmallerWindows() {
        assertEquals(
            WindowSettings(800f, 600f, true),
            WindowSettings(1400f, 1000f, true).fitToScreen(800f, 600f),
        )
        val smaller = WindowSettings(640f, 480f)
        assertEquals(smaller, smaller.fitToScreen(1920f, 1080f))
    }

    private class MemorySettingsRepository(private val beforeWrite: suspend () -> Unit = {}) :
        AppSettingsRepository {
        private val settings = MutableStateFlow(AppSettings())
        var writes = 0
        var failWrites = false
        override suspend fun getSettings() = settings.value
        override fun observeSettings() = settings
        override suspend fun updateWindowSettings(window: WindowSettings) {
            check(!failWrites) { "Test write failure" }
            beforeWrite()
            writes++
            settings.value = settings.value.copy(window = window)
        }
    }
}
