package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

/** Lets features read and change device preferences independently of their storage. */
interface AppSettingsRepository {
    suspend fun getSettings(): AppSettings
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateWindowSettings(window: WindowSettings)
}
