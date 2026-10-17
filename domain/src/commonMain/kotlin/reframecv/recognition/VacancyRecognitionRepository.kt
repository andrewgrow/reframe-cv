package reframecv.recognition

/** Carries PDF bytes to a recognizer without exposing local file paths. */
class RecognitionDocument(val fileName: String, val bytes: ByteArray)

/** Identifies processing failures independently of a provider's HTTP or SDK types. */
enum class RecognitionError {
    NotConfigured,
    UnreadableFile,
    UnsupportedFile,
    InvalidPdf,
    ChangedFile,
    NotRecognized,
    ServiceUnavailable,
    InvalidResponse,
}

/** Reports recognized posting fields or an actionable processing failure. */
sealed interface RecognitionOutcome {
    data class Recognized(val result: RecognizedVacancy) : RecognitionOutcome
    data class Failed(val error: RecognitionError, val details: String? = null) : RecognitionOutcome
}

/** Defines remote vacancy recognition without coupling application features to networking. */
interface VacancyRecognitionRepository {
    suspend fun recognize(document: RecognitionDocument): RecognitionOutcome
}
