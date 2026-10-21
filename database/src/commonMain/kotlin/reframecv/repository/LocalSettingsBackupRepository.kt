package reframecv.repository

import reframecv.database.backup.SettingsBackupDao
import reframecv.settings.SettingsBackup
import reframecv.settings.SettingsBackupRepository

/** Adapts portable settings backups to transactional local storage. */
class LocalSettingsBackupRepository(private val dao: SettingsBackupDao) : SettingsBackupRepository {
    override suspend fun snapshot() = dao.snapshot()
    override suspend fun restore(backup: SettingsBackup) = dao.restore(backup)
}
