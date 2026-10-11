package reframecv.window

import io.klogging.noCoLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import reframecv.domain.models.settings.WindowSettings
import reframecv.repository.AppSettingsRepository

/** Remembers window size and maximized state so the next launch restores them. */
internal class DesktopWindowSettingsStore private constructor(
    private val repository: AppSettingsRepository,
    initial: WindowSettings,
) {
    private val logger = noCoLogger("DesktopWindowSettings")
    private val saveMutex = Mutex()
    private var saved = initial

    @Volatile
    var current = initial
        private set

    fun update(settings: WindowSettings) {
        current = settings
    }

    /** Serialize writes and read the latest value inside the lock, including the close-time save. */
    // Repository failures depend on the storage implementation; preference writes must not close the app.
    @Suppress("TooGenericExceptionCaught")
    suspend fun save() = withContext(Dispatchers.IO) {
        saveMutex.withLock {
            val latest = current
            if (latest != saved) {
                try {
                    repository.updateWindowSettings(latest)
                    saved = latest
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    logger.warn("Could not save window settings: {message}", error.message)
                }
            }
        }
    }

    companion object {
        suspend fun load(repository: AppSettingsRepository): DesktopWindowSettingsStore {
            val initial = repository.getSettings().window
            return DesktopWindowSettingsStore(repository, initial)
        }
    }
}
