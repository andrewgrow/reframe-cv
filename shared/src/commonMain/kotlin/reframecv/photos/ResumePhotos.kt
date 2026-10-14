package reframecv.photos

import reframecv.attachments.SelectedAttachment

/** Stores draft sources and print-ready portraits without changing original images. */
interface ResumePhotoStorage {
    suspend fun prepareSource(selection: SelectedAttachment): String
    suspend fun copySource(path: String): String
    suspend fun render(path: String, crop: PhotoCrop): String
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
    suspend fun copySource(path: String): String
    suspend fun render(path: String, crop: PhotoCrop): String
    suspend fun read(path: String): ByteArray?
    suspend fun discard(path: String)
}
