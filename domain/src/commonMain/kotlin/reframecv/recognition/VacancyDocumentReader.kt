package reframecv.recognition

import reframecv.attachments.SelectedAttachment
import reframecv.domain.models.vacancyattachment.VacancyAttachment

/** Distinguishes an unsaved selection from an immutable application-managed PDF. */
sealed interface RecognitionSource {
    data class Selected(val file: SelectedAttachment) : RecognitionSource
    data class Saved(val attachment: VacancyAttachment) : RecognitionSource
}

/** Reports a file preparation failure without exposing platform exceptions to the UI. */
class RecognitionFileException(val reason: RecognitionError) : Exception(reason.name)

/** Reads a PDF using platform file access before the remote repository receives its bytes. */
fun interface VacancyDocumentReader {
    suspend fun read(source: RecognitionSource): RecognitionDocument
}
