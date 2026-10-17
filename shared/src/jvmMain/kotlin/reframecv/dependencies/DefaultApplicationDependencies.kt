package reframecv.dependencies

import reframecv.attachments.AttachmentPicker
import reframecv.attachments.AttachmentStorage
import reframecv.attachments.DesktopAttachmentPicker
import reframecv.attachments.FileAttachmentStorage
import reframecv.database.AppDatabase
import reframecv.database.openDefaultDatabase
import reframecv.export.DesktopResumeExporter
import reframecv.export.DesktopVacancyExporter
import reframecv.export.ResumeExporter
import reframecv.export.VacancyExporter
import reframecv.filesystem.applicationDataDirectory
import reframecv.importing.DesktopResumeImporter
import reframecv.importing.ResumeImporter
import reframecv.logging.ApplicationLogger
import reframecv.logging.EventHistory
import reframecv.logging.FileApplicationLogger
import reframecv.logging.createHistoryStoreFactory
import reframecv.photos.DesktopResumePhotoPicker
import reframecv.photos.DesktopResumePhotoStorage
import reframecv.photos.ResumePhotos
import reframecv.repository.AppSettingsRepository
import reframecv.repository.CoverLettersRepository
import reframecv.repository.LocalAppSettingsRepository
import reframecv.repository.LocalCoverLettersRepository
import reframecv.repository.LocalProjectsRepository
import reframecv.repository.LocalResumePhotos
import reframecv.repository.LocalResumesRepository
import reframecv.repository.LocalVacanciesRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository
import reframecv.repository.VacancySnapshotManager

/** Gives all screens access to the same local data without reopening the database. */
class DefaultApplicationDependencies(
    override val eventHistory: EventHistory = EventHistory(),
    override val logger: ApplicationLogger = FileApplicationLogger(history = eventHistory),
    override val attachmentStorage: AttachmentStorage =
        FileAttachmentStorage(applicationDataDirectory()),
    override val attachmentPicker: AttachmentPicker = DesktopAttachmentPicker(),
    override val resumeExporter: ResumeExporter = DesktopResumeExporter(),
    override val vacancyExporter: VacancyExporter = DesktopVacancyExporter(),
    override val resumeImporter: ResumeImporter = DesktopResumeImporter(),
    databaseFactory: () -> AppDatabase = ::openDefaultDatabase,
) : ApplicationDependencies {
    override val storeFactory = createHistoryStoreFactory(eventHistory)
    private val database = lazy(databaseFactory)
    private val repository by lazy {
        LocalProjectsRepository(database.value.projectDao(), snapshotManager, photoManager)
    }
    private val resumeRepository by lazy {
        LocalResumesRepository(database.value.resumeDao(), photoManager)
    }
    private val photoManager by lazy {
        LocalResumePhotos(
            database.value.resumeDao(),
            DesktopResumePhotoStorage(applicationDataDirectory()),
            DesktopResumePhotoPicker(),
            logger,
        )
    }
    override val resumePhotos: ResumePhotos get() = photoManager
    private val snapshotManager by lazy {
        VacancySnapshotManager(
            database.value.vacancyDao(),
            database.value.vacancyAttachmentDao(),
            attachmentStorage,
            logger,
        )
    }
    private val vacancyRepository by lazy {
        LocalVacanciesRepository(database.value.vacancyDao(), snapshotManager)
    }
    private val coverLetterRepository by lazy {
        LocalCoverLettersRepository(database.value.coverLetterDao())
    }
    private val settingsRepository by lazy {
        LocalAppSettingsRepository(database.value.appSettingsDao())
    }
    private var closed = false

    override val appSettingsRepository: AppSettingsRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return settingsRepository
        }

    override val projectsRepository: ProjectsRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return repository
        }

    override val resumesRepository: ResumesRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return resumeRepository
        }

    override val vacanciesRepository: VacanciesRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return vacancyRepository
        }

    override val coverLettersRepository: CoverLettersRepository
        get() {
            check(!closed) { "Application dependencies are closed" }
            return coverLetterRepository
        }

    override fun close() {
        if (closed) return
        closed = true
        if (database.isInitialized()) database.value.close()
    }
}
