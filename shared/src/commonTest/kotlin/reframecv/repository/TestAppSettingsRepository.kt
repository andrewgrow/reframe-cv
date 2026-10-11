package reframecv.repository

import kotlinx.coroutines.flow.MutableStateFlow
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

class TestAppSettingsRepository(initial: AppSettings = AppSettings()) : AppSettingsRepository {
    private val settings = MutableStateFlow(initial)
    override suspend fun getSettings() = settings.value
    override fun observeSettings() = settings
    override suspend fun updateWindowSettings(window: WindowSettings) {
        settings.value = settings.value.copy(window = window)
    }
}
