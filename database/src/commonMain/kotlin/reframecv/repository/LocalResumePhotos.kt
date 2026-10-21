package reframecv.repository

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import reframecv.database.project.ProjectDao
import reframecv.database.resume.ResumeDao
import reframecv.domain.models.resume.Resume
import reframecv.logging.ApplicationLogger
import reframecv.photos.PhotoCrop
import reframecv.photos.ResumePhotoPicker
import reframecv.photos.ResumePhotoStorage
import reframecv.photos.ResumePhotos

/** Coordinates portrait writes and cleanup so draft disposal cannot remove committed photos. */
class LocalResumePhotos(
    private val resumes: ResumeDao,
    private val storage: ResumePhotoStorage,
    private val picker: ResumePhotoPicker,
    private val logger: ApplicationLogger,
) : ResumePhotos {
    private val mutex = Mutex()

    override suspend fun choose(): String? = picker.pick()?.let { storage.prepareSource(it) }

    override suspend fun copySource(path: String) = storage.copySource(path)

    override suspend fun render(path: String, crop: PhotoCrop) = storage.render(path, crop)

    override suspend fun read(path: String) = storage.read(path)

    override suspend fun discard(path: String) = mutex.withLock {
        withContext(NonCancellable) { removeUnused(path) }
    }

    suspend fun <T> save(record: Resume, action: suspend () -> T): T = mutex.withLock {
        withContext(NonCancellable) {
            val previous = resumes.findById(record.id)?.entity?.photoPath
            val result = action()
            previous?.takeIf { it != record.photoPath }?.let { removeUnused(it) }
            result
        }
    }

    suspend fun delete(id: Long, action: suspend () -> Unit) = mutex.withLock {
        withContext(NonCancellable) {
            val path = resumes.findById(id)?.entity?.photoPath
            action()
            path?.let { removeUnused(it) }
        }
    }

    suspend fun deleteMany(ids: Set<Long>, action: suspend () -> Unit) = mutex.withLock {
        withContext(NonCancellable) {
            val paths = ids.mapNotNull { resumes.findById(it)?.entity?.photoPath }.distinct()
            action()
            paths.forEach { removeUnused(it) }
        }
    }

    suspend fun deleteProject(projects: ProjectDao, id: Long, action: suspend () -> Int): Int =
        mutex.withLock {
            withContext(NonCancellable) {
                val paths = projects.subtreePhotoPaths(id)
                val result = action()
                paths.forEach { removeUnused(it) }
                result
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun removeUnused(path: String) {
        try {
            if (!resumes.referencesPhoto(path)) storage.delete(path)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.error("Remove unused resume photo", error, mapOf("photoPath" to path))
        }
    }
}
