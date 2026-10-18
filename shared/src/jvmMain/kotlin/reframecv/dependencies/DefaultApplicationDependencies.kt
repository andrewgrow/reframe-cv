package reframecv.dependencies

import reframecv.attachments.AttachmentPicker
import reframecv.attachments.AttachmentStorage
import reframecv.attachments.DesktopAttachmentPicker
import reframecv.attachments.FileAttachmentStorage
import reframecv.database.AppDatabase
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.domain.models.providers.ApiProviderType
import reframecv.export.DesktopResumeExporter
import reframecv.export.DesktopVacancyExporter
import reframecv.export.ResumeExporter
import reframecv.export.VacancyExporter
import reframecv.filesystem.applicationDataDirectory
import reframecv.importing.DesktopResumeImporter
import reframecv.importing.DesktopVacancyImporter
import reframecv.importing.ResumeImporter
import reframecv.importing.VacancyImporter
import reframecv.logging.ApplicationLogger
import reframecv.logging.EventHistory
import reframecv.logging.FileApplicationLogger
import reframecv.logging.createHistoryStoreFactory
import reframecv.network.FileHttpTrafficLogger
import reframecv.network.createDesktopNetworkClient
import reframecv.photos.DesktopResumePhotoPicker
import reframecv.photos.DesktopResumePhotoStorage
import reframecv.photos.ResumePhotos
import reframecv.providers.RemoteProviderAccountRepository
import reframecv.providers.RemoteProviderModelsRepository
import reframecv.providers.openrouter.OpenRouterClient
import reframecv.providers.openrouter.OpenRouterSetupClient
import reframecv.providers.recognition.RemoteVacancyRecognitionRepository
import reframecv.providers.recognition.vacancyRecognitionSchema
import reframecv.recognition.DesktopVacancyDocumentReader
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionRepository
import reframecv.repository.AppSettingsRepository
import reframecv.repository.CoverLettersRepository
import reframecv.repository.LocalAppSettingsRepository
import reframecv.repository.LocalCoverLettersRepository
import reframecv.repository.LocalProjectsRepository
import reframecv.repository.LocalPromptsRepository
import reframecv.repository.LocalProviderConnectionsRepository
import reframecv.repository.LocalResumePhotos
import reframecv.repository.LocalResumesRepository
import reframecv.repository.LocalVacanciesRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository
import reframecv.repository.VacancySnapshotManager
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.ProjectsUseCases
import reframecv.usecase.PromptsUseCases
import reframecv.usecase.ProviderSettingsUseCases
import reframecv.usecase.RecognizeVacancyUseCase
import reframecv.usecase.ResumeFilesUseCases
import reframecv.usecase.ResumesUseCases
import reframecv.usecase.VacanciesUseCases
import reframecv.usecase.VacancyFilesUseCases

/** Gives all screens access to the same local data without reopening the database. */
class DefaultApplicationDependencies(
    override val eventHistory: EventHistory = EventHistory(),
    override val logger: ApplicationLogger = FileApplicationLogger(history = eventHistory),
    override val attachmentStorage: AttachmentStorage =
        FileAttachmentStorage(applicationDataDirectory()),
    override val attachmentPicker: AttachmentPicker = DesktopAttachmentPicker(),
    override val resumeExporter: ResumeExporter = DesktopResumeExporter(),
    override val vacancyExporter: VacancyExporter = DesktopVacancyExporter(),
    override val vacancyImporter: VacancyImporter = DesktopVacancyImporter(),
    override val vacancyDocumentReader: VacancyDocumentReader =
        DesktopVacancyDocumentReader(applicationDataDirectory()),
    override val resumeImporter: ResumeImporter = DesktopResumeImporter(),
    databaseFactory: () -> AppDatabase = {
        buildDatabase(createDatabaseBuilder(applicationDataDirectory().resolve("reframe-cv.db")))
    },
) : ApplicationDependencies {
    override val storeFactory = createHistoryStoreFactory(eventHistory)
    private val database = lazy(databaseFactory)
    private val transport = lazy { createDesktopNetworkClient() }
    private val provider = lazy {
        OpenRouterClient(
            transport.value,
            FileHttpTrafficLogger(applicationDataDirectory().resolve("openrouter-http.log")),
        )
    }
    private val recognitionRepository = lazy {
        RemoteVacancyRecognitionRepository(provider.value, vacancyRecognitionSchema())
    }
    override val vacancyRecognitionRepository: VacancyRecognitionRepository
        get() {
            check(!closed)
            return recognitionRepository.value
        }
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

    private val projectsUseCasesInstance by lazy { ProjectsUseCases(projectsRepository) }
    override val projectsUseCases: ProjectsUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return projectsUseCasesInstance
        }

    private val resumesUseCasesInstance by lazy { ResumesUseCases(resumesRepository) }
    override val resumesUseCases: ResumesUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return resumesUseCasesInstance
        }

    private val vacanciesUseCasesInstance by lazy { VacanciesUseCases(vacanciesRepository) }
    override val vacanciesUseCases: VacanciesUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return vacanciesUseCasesInstance
        }

    private val observeDashboardInstance by lazy {
        ObserveDashboardUseCase(resumesRepository, vacanciesRepository, coverLettersRepository)
    }
    override val observeDashboard: ObserveDashboardUseCase
        get() {
            check(!closed) { "Application dependencies are closed" }
            return observeDashboardInstance
        }

    private val recognizeVacancyInstance by lazy {
        RecognizeVacancyUseCase(
            vacancyDocumentReader,
            vacancyRecognitionRepository,
            providerSettingsUseCases,
        )
    }
    override val recognizeVacancy: RecognizeVacancyUseCase
        get() {
            check(!closed) { "Application dependencies are closed" }
            return recognizeVacancyInstance
        }

    private val resumeFilesUseCasesInstance by lazy {
        ResumeFilesUseCases(resumeImporter, resumeExporter, resumePhotos)
    }
    override val resumeFilesUseCases: ResumeFilesUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return resumeFilesUseCasesInstance
        }

    private val vacancyFilesUseCasesInstance by lazy {
        VacancyFilesUseCases(vacancyImporter, vacancyExporter, attachmentPicker)
    }
    override val vacancyFilesUseCases: VacancyFilesUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return vacancyFilesUseCasesInstance
        }

    private val prompts by lazy {
        PromptsUseCases(LocalPromptsRepository(database.value.promptsDao()))
    }
    override val promptsUseCases: PromptsUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return prompts
        }

    private val providerSettings by lazy {
        val client = OpenRouterSetupClient(
            transport.value,
            FileHttpTrafficLogger(applicationDataDirectory().resolve("openrouter-http.log")),
        )
        ProviderSettingsUseCases(
            LocalProviderConnectionsRepository(database.value.providerConnectionsDao()),
            RemoteProviderModelsRepository(mapOf(ApiProviderType.OpenRouter to client)),
            RemoteProviderAccountRepository(mapOf(ApiProviderType.OpenRouter to client)),
        )
    }
    override val providerSettingsUseCases: ProviderSettingsUseCases
        get() {
            check(!closed) { "Application dependencies are closed" }
            return providerSettings
        }

    override fun close() {
        if (closed) return
        closed = true
        if (transport.isInitialized()) {
            transport.value.close()
            transport.value.engine.close()
        }
        if (database.isInitialized()) database.value.close()
    }
}
