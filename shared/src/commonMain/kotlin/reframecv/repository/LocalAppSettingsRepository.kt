package reframecv.repository

import kotlinx.coroutines.flow.map
import reframecv.database.settings.AppSettingsDao
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

/** Restores device preferences from local storage or supplies first-launch defaults. */
class LocalAppSettingsRepository(private val dao: AppSettingsDao) : AppSettingsRepository {
    override suspend fun getSettings() = dao.get()?.toDomainModel() ?: AppSettings()
    override fun observeSettings() = dao.observe().map { it?.toDomainModel() ?: AppSettings() }
    override suspend fun updateWindowSettings(window: WindowSettings) {
        dao.saveWindow(window.width, window.height, window.maximized)
    }
}
