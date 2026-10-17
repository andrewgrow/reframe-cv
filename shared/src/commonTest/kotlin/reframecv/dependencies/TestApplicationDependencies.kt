package reframecv.dependencies

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.TestAttachmentStorage
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
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
import reframecv.repository.TestAppSettingsRepository
import reframecv.repository.TestCoverLettersRepository
import reframecv.repository.TestResumesRepository
import reframecv.repository.TestVacanciesRepository
import reframecv.shared.nowMillis

class TestApplicationDependencies : ApplicationDependencies {
    override val eventHistory = EventHistory()
    override val storeFactory = createHistoryStoreFactory(eventHistory)
    override val logger = RecordingApplicationLogger()
    override val attachmentStorage = TestAttachmentStorage()
    override val attachmentPicker = AttachmentPicker { null }
    override val vacancyDocumentReader =
        VacancyDocumentReader { RecognitionDocument("job.pdf", "%PDF-1.7".encodeToByteArray()) }
    override val vacancyRecognitionRepository = object : VacancyRecognitionRepository {
        override suspend fun recognize(document: RecognitionDocument) =
            RecognitionOutcome.Failed(RecognitionError.NotConfigured)
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

    override fun close() = Unit
}
