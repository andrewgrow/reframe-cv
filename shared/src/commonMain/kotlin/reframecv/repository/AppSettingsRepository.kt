package reframecv.repository

import kotlinx.coroutines.flow.Flow
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

interface AppSettingsRepository {
    suspend fun getSettings(): AppSettings
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateWindowSettings(window: WindowSettings)
}
