package reframecv.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import reframecv.database.coverletter.CoverLetterDao
import reframecv.database.coverletter.CoverLetterEntity
import reframecv.database.coverletter.CoverLetterKeywordEntity
import reframecv.database.project.ProjectDao
import reframecv.database.project.ProjectEntity
import reframecv.database.prompts.PromptEntity
import reframecv.database.prompts.PromptsDao
import reframecv.database.providers.ProviderConnectionEntity
import reframecv.database.providers.ProviderConnectionsDao
import reframecv.database.resume.ResumeDao
import reframecv.database.resume.ResumeEntity
import reframecv.database.resume.ResumeKeywordEntity
import reframecv.database.settings.AppSettingsDao
import reframecv.database.settings.AppSettingsEntity
import reframecv.database.vacancy.VacancyDao
import reframecv.database.vacancy.VacancyEntity
import reframecv.database.vacancy.VacancyKeywordEntity
import reframecv.database.vacancyattachment.VacancyAttachmentDao
import reframecv.database.vacancyattachment.VacancyAttachmentEntity

/** Keeps projects, workspace documents, and device preferences in one local database. */
@Database(
    entities = [
        AppSettingsEntity::class,
        ProviderConnectionEntity::class,
        PromptEntity::class,
        ProjectEntity::class,
        VacancyAttachmentEntity::class,
        ResumeEntity::class, ResumeKeywordEntity::class,
        CoverLetterEntity::class, CoverLetterKeywordEntity::class,
        VacancyEntity::class, VacancyKeywordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptsDao(): PromptsDao
    abstract fun providerConnectionsDao(): ProviderConnectionsDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun vacancyAttachmentDao(): VacancyAttachmentDao
    abstract fun projectDao(): ProjectDao
    abstract fun resumeDao(): ResumeDao
    abstract fun coverLetterDao(): CoverLetterDao
    abstract fun vacancyDao(): VacancyDao
}

/** Enables Room to create the shared database on each supported platform. */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun buildDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
