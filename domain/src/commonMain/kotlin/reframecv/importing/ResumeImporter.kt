package reframecv.importing

import reframecv.domain.models.resume.content.ResumeContent

/** Holds imported fields and the temporary source until the editor releases it. */
interface ImportedResumeFile {
    val content: ResumeContent
    suspend fun deleteTemporaryFile()
}

/** Lets resume creation choose and parse a file through platform-specific file handling. */
fun interface ResumeImporter {
    suspend fun pickAndImport(): ImportedResumeFile?
}
