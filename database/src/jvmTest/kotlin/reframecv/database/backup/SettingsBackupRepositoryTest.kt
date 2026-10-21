package reframecv.database.backup

import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings
import reframecv.repository.LocalSettingsBackupRepository
import reframecv.settings.SettingsBackup
import reframecv.settings.SettingsBackupJson

class SettingsBackupRepositoryTest : WorkspaceDatabaseTest() {
    private val backup = SettingsBackup(
        AppSettings(WindowSettings(1100f, 720f, true)),
        listOf(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Home",
                apiKey = "api",
                managementApiKey = "management",
                modelId = "model",
                active = false,
            ),
        ),
        listOf(Prompt(text = "Read", active = false)),
    )

    @Test
    fun ownExportRestoresIntoFreshDatabaseAndRepeatedImportsDoNotDuplicate() = runBlocking<Unit> {
        val repository = LocalSettingsBackupRepository(database.settingsBackupDao())
        repository.restore(backup)
        val json = SettingsBackupJson.encode(repository.snapshot(), true)
        val restoredDatabase =
            buildDatabase(createDatabaseBuilder(temporaryFolder.newFile("restored.db").toPath()))
        try {
            val target = LocalSettingsBackupRepository(restoredDatabase.settingsBackupDao())
            val report = target.restore(SettingsBackupJson.decode(json))
            assertEquals(1, report.connections)
            assertEquals(1, report.prompts)
            assertEquals(backup.settings, target.snapshot().settings)
            val connection = target.snapshot().connections.single()
            assertEquals("management", connection.managementApiKey)
            assertEquals(false, connection.active)
            assertNull(connection.accountInfo)
            val again = target.restore(SettingsBackupJson.decode(json))
            assertEquals(2, again.duplicates)
            assertEquals(0, again.connections)
            assertEquals(0, again.prompts)
        } finally {
            restoredDatabase.close()
        }
    }

    @Test
    fun omittedKeysNeverEraseCredentialsAndChangedRecordsAreAdded() = runBlocking<Unit> {
        val repository = LocalSettingsBackupRepository(database.settingsBackupDao())
        repository.restore(backup)
        val withoutKeys = SettingsBackupJson.decode(SettingsBackupJson.encode(backup, false))
        assertEquals(2, repository.restore(withoutKeys).duplicates)
        assertEquals("api", repository.snapshot().connections.single().apiKey)
        val changed = backup.copy(
            connections = listOf(backup.connections.single().copy(name = "Another")),
            prompts = listOf(backup.prompts.single().copy(text = "Different")),
            skipped = 3,
        )
        assertEquals(3, repository.restore(changed).skipped)
        assertEquals(2, repository.snapshot().connections.size)
        assertEquals(2, repository.snapshot().prompts.size)
    }

    @Test
    fun failedImportRollsBackPreviouslyInsertedRecordsAndPreservesWindow() = runBlocking<Unit> {
        val repository = LocalSettingsBackupRepository(database.settingsBackupDao())
        database.useWriterConnection { connection ->
            connection.executeSQL(
                "CREATE TRIGGER reject_prompt BEFORE INSERT ON prompts BEGIN SELECT RAISE(ABORT, 'Test failure'); END",
            )
        }
        assertFailsWith<SQLiteException> { repository.restore(backup) }
        assertEquals(SettingsBackup(AppSettings()), repository.snapshot())
        assertNull(database.appSettingsDao().get())
    }
}
