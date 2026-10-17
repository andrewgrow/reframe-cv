package reframecv.database.settings

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import reframecv.database.DatabaseModel
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

internal const val APP_SETTINGS_ID = 1

/** Represents the device preferences record in the local database. */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = APP_SETTINGS_ID,
    @ColumnInfo(name = "window_width") val windowWidth: Float = 800f,
    @ColumnInfo(name = "window_height") val windowHeight: Float = 600f,
    @ColumnInfo(name = "window_maximized") val windowMaximized: Boolean = false,
) : DatabaseModel<AppSettings> {
    init {
        require(id == APP_SETTINGS_ID) { "Application settings use a single row" }
    }

    override fun toDomainModel(): AppSettings {
        val defaults = WindowSettings()
        return AppSettings(
            WindowSettings(
                width = windowWidth.takeIf { it.isFinite() && it > 0f } ?: defaults.width,
                height = windowHeight.takeIf { it.isFinite() && it > 0f } ?: defaults.height,
                maximized = windowMaximized,
            ),
        )
    }
}
