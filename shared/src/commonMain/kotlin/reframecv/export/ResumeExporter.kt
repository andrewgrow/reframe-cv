package reframecv.export

import reframecv.domain.models.resume.Resume

/** Reports whether the generated document was saved or the file picker was cancelled. */
enum class ResumeExportResult { Saved, Cancelled }

/** Lets resume features export documents through platform-specific file handling. */
fun interface ResumeExporter {
    suspend fun export(resume: Resume): ResumeExportResult
}
