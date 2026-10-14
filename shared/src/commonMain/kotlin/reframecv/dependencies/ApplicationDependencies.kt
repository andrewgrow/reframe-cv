package reframecv.dependencies

import com.arkivanov.mvikotlin.core.store.StoreFactory
import reframecv.attachments.AttachmentPicker
import reframecv.attachments.AttachmentStorage
import reframecv.export.ResumeExporter
import reframecv.logging.ApplicationLogger
import reframecv.logging.EventHistory
import reframecv.repository.AppSettingsRepository
import reframecv.repository.CoverLettersRepository
import reframecv.repository.ProjectsRepository
import reframecv.repository.ResumesRepository
import reframecv.repository.VacanciesRepository

/** Defines the contract each platform implements to provide application dependencies. */
interface ApplicationDependencies {
    val eventHistory: EventHistory
    val storeFactory: StoreFactory
    val logger: ApplicationLogger
    val attachmentStorage: AttachmentStorage
    val attachmentPicker: AttachmentPicker
    val resumeExporter: ResumeExporter
    val appSettingsRepository: AppSettingsRepository
    val projectsRepository: ProjectsRepository
    val resumesRepository: ResumesRepository
    val vacanciesRepository: VacanciesRepository
    val coverLettersRepository: CoverLettersRepository
    fun close()
}
