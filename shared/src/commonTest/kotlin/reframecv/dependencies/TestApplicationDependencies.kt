package reframecv.dependencies

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.TestAttachmentStorage
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.export.DocumentExportResult
import reframecv.export.ResumeExportResult
import reframecv.export.ResumeExporter
import reframecv.export.VacancyExporter
import reframecv.importing.ResumeImporter
import reframecv.importing.VacancyImporter
import reframecv.logging.EventHistory
import reframecv.logging.RecordingApplicationLogger
import reframecv.logging.createHistoryStoreFactory
import reframecv.photos.PhotoCrop
import reframecv.photos.ResumePhotos
import reframecv.recognition.RecognitionDocument
import reframecv.recognition.RecognitionError
import reframecv.recognition.RecognitionOutcome
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.repository.TestAppSettingsRepository
import reframecv.repository.TestCoverLettersRepository
import reframecv.repository.TestPromptsRepository
import reframecv.repository.TestResumesRepository
import reframecv.repository.TestScreenStatesRepository
import reframecv.repository.TestVacanciesRepository
import reframecv.shared.nowMillis
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.ProjectsUseCases
import reframecv.usecase.PromptsUseCases
import reframecv.usecase.ProviderSettingsUseCases
import reframecv.usecase.RecognizeVacancyUseCase
import reframecv.usecase.ResumeFilesUseCases
import reframecv.usecase.ResumesUseCases
import reframecv.usecase.ScreenStateUseCases
import reframecv.usecase.VacanciesUseCases
import reframecv.usecase.VacancyFilesUseCases

class TestApplicationDependencies : ApplicationDependencies {
    override val eventHistory = EventHistory()
    override val storeFactory = createHistoryStoreFactory(eventHistory)
    override val logger = RecordingApplicationLogger()
    override val attachmentStorage = TestAttachmentStorage()
    override val attachmentPicker = AttachmentPicker { null }
    override val vacancyDocumentReader =
        VacancyDocumentReader { RecognitionDocument("job.pdf", "%PDF-1.7".encodeToByteArray()) }
    override val vacancyRecognitionRepository = object : VacancyRecognitionRepository {
        override suspend fun recognize(
            document: RecognitionDocument,
            connection: reframecv.domain.models.providers.ProviderConnection,
            instruction: String,
        ) = RecognitionOutcome.Failed(RecognitionError.NotConfigured)
    }
    override val vacancyExporter = VacancyExporter { DocumentExportResult.Cancelled }
    override val resumeExporter = ResumeExporter { ResumeExportResult.Cancelled }
    override val vacancyImporter = VacancyImporter { null }
    override val resumeImporter = ResumeImporter { null }
    override val resumePhotos = object : ResumePhotos {
        override suspend fun choose(): String? = null
        override suspend fun copySource(path: String) = path
        override suspend fun render(path: String, crop: PhotoCrop) = path
        override suspend fun read(path: String): ByteArray? = null
        override suspend fun discard(path: String) = Unit
    }
    override val appSettingsRepository = TestAppSettingsRepository()
    override val projectsRepository = object : ProjectsRepository {
        private val projects = MutableStateFlow<List<Project>>(emptyList())
        override fun observeAllProjects() = projects.map { values ->
            values.sortedBy { it.id }
        }

        override fun observeProjects(parentId: Long?) = projects.map { values ->
            values.filter { it.parentId == parentId }.sortedBy { it.id }
        }

        override suspend fun deleteProject(id: Long) {
            val parentId = projects.value.find { it.id == id }?.parentId
            val ids = mutableSetOf(id)
            do {
                val added = projects.value.filter { it.parentId in ids }.map { it.id }
                val changed = ids.addAll(added)
            } while (changed)
            val remaining = projects.value.filterNot { it.id in ids }
            projects.value = remaining.map {
                if (it.id == parentId && it.mode == ProjectMode.Container &&
                    remaining.none { child -> child.parentId == parentId }
                ) {
                    it.copy(mode = ProjectMode.Unconfigured, updatedAt = nowMillis())
                } else {
                    it
                }
            }
        }

        override suspend fun updateProject(id: Long, name: String) {
            projects.value = projects.value.map { project ->
                if (project.id ==
                    id
                ) {
                    project.copy(name = name.trim(), updatedAt = nowMillis())
                } else {
                    project
                }
            }
        }

        override suspend fun createProject(name: String, parentId: Long?): Project {
            require(name.isNotBlank())
            val parent = parentId?.let { id ->
                checkNotNull(projects.value.find { it.id == id })
                    .also { check(it.mode != ProjectMode.Workspace) }
            }
            val now = nowMillis()
            val project = Project(
                id = projects.value.size.toLong() + 1,
                name = name.trim(),
                parentId = parentId,
                createdAt = now,
                updatedAt = now,
            )
            projects.value = projects.value.map {
                if (it.id == parent?.id) {
                    it.copy(mode = ProjectMode.Container, updatedAt = now)
                } else {
                    it
                }
            } + project
            return project
        }
    }

    override val resumesRepository = TestResumesRepository()

    override val vacanciesRepository = TestVacanciesRepository()

    override val coverLettersRepository = TestCoverLettersRepository()

    override val projectsUseCases get() = ProjectsUseCases(projectsRepository)
    override val resumesUseCases get() = ResumesUseCases(resumesRepository)
    override val vacanciesUseCases get() = VacanciesUseCases(vacanciesRepository)
    override val observeDashboard get() = ObserveDashboardUseCase(
        resumesRepository,
        vacanciesRepository,
        coverLettersRepository,
    )
    override val recognizeVacancy get() = RecognizeVacancyUseCase(
        vacancyDocumentReader,
        vacancyRecognitionRepository,
        providerSettingsUseCases,
    )
    override val resumeFilesUseCases get() = ResumeFilesUseCases(
        resumeImporter,
        resumeExporter,
        resumePhotos,
    )
    override val vacancyFilesUseCases get() = VacancyFilesUseCases(
        vacancyImporter,
        vacancyExporter,
        attachmentPicker,
    )

    val promptsRepository = TestPromptsRepository()
    override val promptsUseCases = PromptsUseCases(promptsRepository)

    private val connections = MutableStateFlow<List<ProviderConnection>>(emptyList())
    override val providerSettingsUseCases = ProviderSettingsUseCases(
        object : ProviderConnectionsRepository {
            override fun observe() = connections
            override suspend fun get(id: Long) = connections.value.find { it.id == id }
            override suspend fun save(connection: ProviderConnection): Long {
                val id = connection.id.takeIf { it != 0L }
                    ?: ((connections.value.maxOfOrNull { it.id } ?: 0) + 1)
                connections.value = connections.value.filterNot { it.id == id } +
                    connection.copy(
                        id = id,
                    )
                return id
            }
            override suspend fun updateCreditBalance(
                id: Long,
                type: ApiProviderType,
                managementApiKey: String,
                balance: reframecv.domain.models.providers.ProviderCreditBalance,
            ) {
                connections.value = connections.value.map {
                    val matches =
                        it.id == id && it.type == type && it.managementApiKey == managementApiKey
                    if (matches && it.active) {
                        it.copy(creditBalance = balance)
                    } else {
                        it
                    }
                }
            }
            override suspend fun updateAccountInfo(
                id: Long,
                type: ApiProviderType,
                apiKey: String,
                info: ProviderAccountInfo,
            ) {
                connections.value = connections.value.map {
                    if (it.id == id && it.type == type &&
                        it.apiKey == apiKey
                    ) {
                        it.copy(accountInfo = info)
                    } else {
                        it
                    }
                }
            }
            override suspend fun setActive(id: Long, active: Boolean) {
                connections.value =
                    connections.value.map { if (it.id == id) it.copy(active = active) else it }
            }
        },
        ProviderModelsRepository { _, _ -> emptyList() },
        ProviderAccountRepository { _, _ ->
            ProviderAccountInfo(fetchedAt = reframecv.shared.nowMillis())
        },
    )
    val screenStatesRepository = TestScreenStatesRepository()
    private val screenStates = lazy { ScreenStateUseCases(screenStatesRepository, logger) }
    override val screenStateUseCases get() = screenStates.value
    override fun close() {
        if (screenStates.isInitialized()) runBlocking { screenStates.value.close() }
    }
}
