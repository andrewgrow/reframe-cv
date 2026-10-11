package reframecv.attachments

/** Describes a file selected on the current device before it is copied. */
data class SelectedAttachment(val source: String, val fileName: String)

/** Describes the application's immutable copy of a source file. */
data class StoredAttachment(
    val filePath: String,
    val fileName: String,
    val mediaType: String,
    val fileSize: Long,
    val sha256: String,
)

/** Reports whether a stored snapshot can still be used. */
enum class AttachmentStatus { Available, Missing, Changed }

/** Defines platform file storage used by vacancy snapshots. */
interface AttachmentStorage {
    suspend fun store(selection: SelectedAttachment): StoredAttachment
    suspend fun status(attachment: StoredAttachment): AttachmentStatus
    suspend fun open(filePath: String)
    suspend fun delete(filePath: String)
    suspend fun cleanup(referencedPaths: Set<String>)
}

/** Defines platform selection of a vacancy source file. */
fun interface AttachmentPicker {
    suspend fun pick(): SelectedAttachment?
}

/** Describes how saving a vacancy changes its source snapshot. */
sealed interface SnapshotChange {
    data object Keep : SnapshotChange
    data object Remove : SnapshotChange
    data class Attach(val selection: SelectedAttachment) : SnapshotChange
}
