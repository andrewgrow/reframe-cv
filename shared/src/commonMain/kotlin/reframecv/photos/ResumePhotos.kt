package reframecv.photos

import reframecv.attachments.SelectedAttachment

/** Stores print-ready portrait copies without changing the user's original images. */
interface ResumePhotoStorage {
    suspend fun prepare(selection: SelectedAttachment): String
    suspend fun read(path: String): ByteArray?
    suspend fun delete(path: String)
}

/** Selects an image on the current platform. */
fun interface ResumePhotoPicker {
    suspend fun pick(): SelectedAttachment?
}

/** Gives editors photo preparation and safe disposal of uncommitted copies. */
interface ResumePhotos {
    suspend fun choose(): String?
    suspend fun read(path: String): ByteArray?
    suspend fun discard(path: String)
}
