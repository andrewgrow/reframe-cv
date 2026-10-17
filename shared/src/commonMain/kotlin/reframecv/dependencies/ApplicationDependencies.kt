package reframecv.dependencies

import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.AttachmentStorage
import reframecv.export.ResumeExporter
import reframecv.export.VacancyExporter
import reframecv.importing.ResumeImporter
import reframecv.importing.VacancyImporter
import reframecv.logging.ApplicationLogger
import reframecv.logging.EventHistory
import reframecv.photos.ResumePhotos
import reframecv.recognition.VacancyDocumentReader
import reframecv.recognition.VacancyRecognitionRepository
import reframecv.repository.AppSettingsRepository
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository
import reframecv.usecase.ObserveDashboardUseCase
import reframecv.usecase.ProjectsUseCases
import reframecv.usecase.RecognizeVacancyUseCase
import reframecv.usecase.ResumeFilesUseCases
import reframecv.usecase.ResumesUseCases
import reframecv.usecase.VacanciesUseCases
import reframecv.usecase.VacancyFilesUseCases

/** Defines the contract each platform implements to provide application dependencies. */
interface ApplicationDependencies {
    val eventHistory: EventHistory
    val storeFactory: StoreFactory
    val logger: ApplicationLogger
    val attachmentStorage: AttachmentStorage
    val attachmentPicker: AttachmentPicker
    val vacancyDocumentReader: VacancyDocumentReader
    val vacancyRecognitionRepository: VacancyRecognitionRepository
    val vacancyExporter: VacancyExporter
    val resumeExporter: ResumeExporter
    val vacancyImporter: VacancyImporter
    val resumeImporter: ResumeImporter
    val resumePhotos: ResumePhotos
    val appSettingsRepository: AppSettingsRepository
    val projectsRepository: ProjectsRepository
    val resumesRepository: ResumesRepository
    val vacanciesRepository: VacanciesRepository
    val coverLettersRepository: CoverLettersRepository
    val projectsUseCases: ProjectsUseCases
    val resumesUseCases: ResumesUseCases
    val vacanciesUseCases: VacanciesUseCases
    val observeDashboard: ObserveDashboardUseCase
    val recognizeVacancy: RecognizeVacancyUseCase
    val resumeFilesUseCases: ResumeFilesUseCases
    val vacancyFilesUseCases: VacancyFilesUseCases
    fun close()
}
