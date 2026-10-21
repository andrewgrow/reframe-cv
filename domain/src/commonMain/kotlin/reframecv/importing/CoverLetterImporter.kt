package reframecv.importing

import reframecv.domain.models.coverletter.CoverLetter

/** Holds portable cover-letter fields and owns the temporary JSON source. */
interface ImportedCoverLetterFile {
    val coverLetter: CoverLetter
    suspend fun deleteTemporaryFile()
}

/** Lets cover-letter creation select and read JSON through platform file handling. */
fun interface CoverLetterImporter {
    suspend fun pickAndImport(): ImportedCoverLetterFile?
}
