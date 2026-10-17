package reframecv.database.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings
import reframecv.repository.LocalAppSettingsRepository

class AppSettingsRepositoryTest : WorkspaceDatabaseTest() {
    @Test
    fun emptyDatabaseReturnsDefaultsAndUpdatesPersistAcrossReopening() = runBlocking<Unit> {
        var repository = LocalAppSettingsRepository(database.appSettingsDao())
        assertEquals(AppSettings(), repository.getSettings())
        assertEquals(AppSettings(), repository.observeSettings().first())
        assertNull(database.appSettingsDao().get())
        repository.updateWindowSettings(WindowSettings(1000f, 700f, true))
        repository.updateWindowSettings(WindowSettings(900f, 650f))
        database.close()
        database =
            buildDatabase(
                createDatabaseBuilder(temporaryFolder.root.toPath().resolve("workspace.db")),
            )
        repository = LocalAppSettingsRepository(database.appSettingsDao())
        assertEquals(AppSettings(WindowSettings(900f, 650f)), repository.getSettings())
        assertEquals(APP_SETTINGS_ID, database.appSettingsDao().get()?.id)
        assertEquals(repository.getSettings(), database.appSettingsDao().get()?.toDomainModel())
    }

    @Test
    fun observersReceiveSavedSettingsAndUnrelatedProjectDeletionDoesNotResetThem() =
        runBlocking<Unit> {
            val repository = LocalAppSettingsRepository(database.appSettingsDao())
            val expected = WindowSettings(1200f, 800f, true)
            val observed =
                async {
                    withTimeout(10_000.milliseconds) {
                        repository.observeSettings().first {
                            it.window ==
                                expected
                        }
                    }
                }
            repository.updateWindowSettings(expected)
            assertEquals(AppSettings(expected), observed.await())
            val id = project()
            database.projectDao().deleteSubtree(id)
            assertEquals(AppSettings(expected), repository.getSettings())
        }

    @Test
    fun invalidStoredDimensionsFallbackAndInvalidDomainDimensionsAreRejected() = runBlocking<Unit> {
        database.appSettingsDao().saveWindow(-20f, 0f, true)
        assertEquals(
            AppSettings(WindowSettings(maximized = true)),
            LocalAppSettingsRepository(database.appSettingsDao()).getSettings(),
        )
        assertFailsWith<IllegalArgumentException> { WindowSettings(width = Float.NaN) }
        assertFailsWith<IllegalArgumentException> {
            WindowSettings(height = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> { WindowSettings(width = 0f) }
        assertFailsWith<IllegalArgumentException> { WindowSettings(height = -10f) }
        assertFailsWith<IllegalArgumentException> { AppSettingsEntity(id = 2) }
    }
}
