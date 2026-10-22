package reframecv.dependencies

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import reframecv.assessment.ResumeAssessmentOutcome
import reframecv.assessment.ResumeAssessmentRepository
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.TestAttachmentStorage
import reframecv.coverletters.CoverLetterGenerationOutcome
import reframecv.coverletters.CoverLetterGenerationRepository
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.export.CoverLetterExporter
import reframecv.export.DocumentExportResult
import reframecv.export.ResumeExportResult
import reframecv.export.ResumeExporter
import reframecv.export.VacancyExporter
import reframecv.importing.CoverLetterImporter
import reframecv.importing.ResumeImporter
import reframecv.importing.VacancyImporter
import reframecv.improvement.ResumeImprovementOutcome
import reframecv.improvement.ResumeImprovementRepository
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
import reframecv.ui.components.common.ai.AiRequestBalance
import reframecv.usecase.AssessResumeUseCase
import reframecv.usecase.CoverLetterFilesUseCases
import reframecv.usecase.CoverLettersUseCases
import reframecv.usecase.GenerateCoverLetterUseCase
import reframecv.usecase.ImproveResumeUseCase
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
    override val settingsBackupUseCases = reframecv.usecase.SettingsBackupUseCases(
        object : reframecv.settings.SettingsBackupRepository {
            override suspend fun snapshot() = reframecv.settings.SettingsBackup()
            override suspend fun restore(backup: reframecv.settings.SettingsBackup) =
                reframecv.settings.SettingsImportReport(
                    backup.connections.size,
                    backup.prompts.size,
                )
        },
        object : reframecv.settings.SettingsBackupFiles {
            override suspend fun read(): String? = null
            override suspend fun write(json: String) = DocumentExportResult.Cancelled
        },
    )
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
            connection: ProviderConnection,
            instruction: String,
        ) = RecognitionOutcome.Failed(RecognitionError.NotConfigured)
    }
    var pdfExport: suspend (reframecv.domain.models.coverletter.CoverLetter) ->
    DocumentExportResult = { DocumentExportResult.Cancelled }
    override val coverLetterPdfExporter = CoverLetterExporter { pdfExport(it) }
    override val coverLetterExporter = CoverLetterExporter { DocumentExportResult.Cancelled }
    override val coverLetterImporter = CoverLetterImporter { null }
    override val vacancyExporter = VacancyExporter { DocumentExportResult.Cancelled }
    override val resumePdfExporter = ResumeExporter { ResumeExportResult.Cancelled }
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

    override val vacanciesRepository = TestVacanciesRepository()

    override val resumesRepository = TestResumesRepository(vacanciesRepository)

    override val coverLettersRepository = TestCoverLettersRepository(vacanciesRepository)

    override val projectsUseCases get() = ProjectsUseCases(projectsRepository)
    override val resumesUseCases get() = ResumesUseCases(resumesRepository)
    override val vacanciesUseCases get() = VacanciesUseCases(vacanciesRepository)
    override val observeDashboard get() = ObserveDashboardUseCase(
        resumesRepository,
        vacanciesRepository,
        coverLettersRepository,
    )
    override val coverLettersUseCases get() = CoverLettersUseCases(coverLettersRepository)
    var letterResponse: suspend () -> CoverLetterGenerationOutcome = {
        CoverLetterGenerationOutcome.Failed("Generation unavailable")
    }
    val letterInputs = mutableListOf<Pair<String, String>>()
    override val generateCoverLetter get() = GenerateCoverLetterUseCase(
        CoverLetterGenerationRepository {
                resume,
                vacancy,
                _,
                _,
            ->
            letterInputs += resume to vacancy
            letterResponse()
        },
        providerSettingsUseCases,
        coverLettersRepository,
    )
    val improvementInputs = mutableListOf<Pair<String, String>>()
    var improvementResponse: suspend () -> ResumeImprovementOutcome = {
        ResumeImprovementOutcome.Failed("Improvement unavailable")
    }
    override val improveResume get() = ImproveResumeUseCase(
        ResumeImprovementRepository { resume, vacancy, _, _ ->
            improvementInputs += resume to vacancy
            improvementResponse()
        },
        providerSettingsUseCases,
        resumesRepository,
    )
    val assessmentInputs = mutableListOf<Pair<String, String>>()
    var assessmentResponse: suspend () -> ResumeAssessmentOutcome = {
        ResumeAssessmentOutcome.Failed("Assessment unavailable")
    }
    override val assessResume get() = AssessResumeUseCase(
        ResumeAssessmentRepository { resume, vacancy, _, _ ->
            assessmentInputs += resume to vacancy
            assessmentResponse()
        },
        providerSettingsUseCases,
        vacanciesRepository,
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
        resumePdfExporter,
    )
    override val coverLetterFilesUseCases =
        CoverLetterFilesUseCases(coverLetterImporter, coverLetterExporter, coverLetterPdfExporter)
    override val vacancyFilesUseCases get() = VacancyFilesUseCases(
        vacancyImporter,
        vacancyExporter,
        attachmentPicker,
    )

    val promptsRepository = TestPromptsRepository()
    override val promptsUseCases = PromptsUseCases(promptsRepository)

    val balanceRequests = mutableListOf<String>()
    var balanceResponse: suspend () -> reframecv.domain.models.providers.ProviderCreditBalance = {
        reframecv.domain.models.providers.ProviderCreditBalance(100.0, 0.01, 1)
    }
    private val connections = MutableStateFlow<List<ProviderConnection>>(emptyList())
    val runnerResultsRepository = reframecv.repository.TestRunnerResultsRepository(
        committed = { connectionRunnersRepository.updateRun(it) },
        createVacancy = { vacanciesRepository.create(it) },
    )
    override val runnerResultReviewUseCases by lazy {
        reframecv.usecase.RunnerResultReviewUseCases(
            runnerResultsRepository,
            projectsRepository,
            reframecv.runners.RunnerResultConverter { result, projectId, timestamp ->
                reframecv.domain.models.vacancy.Vacancy(
                    projectId = projectId,
                    name = result.externalId.orEmpty(),
                    createdAt = timestamp,
                    updatedAt = timestamp,
                )
            },
        )
    }
    val runnerExecutionRepository = reframecv.repository.TestRunnerExecutionRepository()
    override val runnerExecutionUseCases by lazy {
        reframecv.usecase.RunnerExecutionUseCases(
            connectionRunnersRepository,
            providerConnectionsRepository,
            runnerExecutionRepository,
            runnerResultsRepository,
        )
    }
    val connectionRunnersRepository = reframecv.repository.TestConnectionRunnersRepository()
    override val connectionRunnersUseCases by lazy {
        reframecv.usecase.ConnectionRunnersUseCases(
            connectionRunnersRepository,
            providerConnectionsRepository,
        )
    }
    private val providerConnectionsRepository = object : ProviderConnectionsRepository {
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
                    it.id == id && it.type == type &&
                        it.openRouter.managementApiKey == managementApiKey
                if (matches && it.active) {
                    it.copy(cachedInfo = it.openRouterCache.copy(creditBalance = balance))
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
                    it.openRouter.apiKey == apiKey
                ) {
                    it.copy(cachedInfo = it.openRouterCache.copy(accountInfo = info))
                } else {
                    it
                }
            }
        }
        override suspend fun setActive(id: Long, active: Boolean) {
            connections.value =
                connections.value.map { if (it.id == id) it.copy(active = active) else it }
        }
    }
    override val providerSettingsUseCases = ProviderSettingsUseCases(
        providerConnectionsRepository,
        ProviderModelsRepository { _, _ -> emptyList() },
        object : ProviderAccountRepository {
            override suspend fun load(type: ApiProviderType, apiKey: String) =
                ProviderAccountInfo(fetchedAt = reframecv.shared.nowMillis())
            override suspend fun balance(
                type: ApiProviderType,
                managementApiKey: String,
            ): reframecv.domain.models.providers.ProviderCreditBalance {
                balanceRequests += managementApiKey
                return balanceResponse()
            }
        },
    )
    val screenStatesRepository = TestScreenStatesRepository()
    private val screenStates = lazy { ScreenStateUseCases(screenStatesRepository, logger) }
    override val screenStateUseCases get() = screenStates.value
    private val requestBalance = lazy { AiRequestBalance(providerSettingsUseCases, logger) }
    override val aiRequestBalance get() = requestBalance.value

    override fun close() {
        if (requestBalance.isInitialized()) requestBalance.value.close()
        if (screenStates.isInitialized()) runBlocking { screenStates.value.close() }
    }
}
