package reframecv.export

import reframecv.domain.models.resume.Resume

/** Reports whether the generated document was saved or the file picker was cancelled. */
typealias ResumeExportResult = DocumentExportResult

/** Lets resume features export documents through platform-specific file handling. */
fun interface ResumeExporter {
    suspend fun export(resume: Resume): ResumeExportResult
}
