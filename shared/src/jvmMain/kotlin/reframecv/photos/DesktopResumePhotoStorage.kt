package reframecv.photos

import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.util.UUID
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import reframecv.attachments.SelectedAttachment

/** Keeps prepared portraits inside the application's document directory. */
class DesktopResumePhotoStorage(directory: Path) : ResumePhotoStorage {
    private val root = directory.toAbsolutePath().normalize()
    private val photos = root.resolve("documents/resume-photos")

    override suspend fun prepareSource(selection: SelectedAttachment): String = createCopy("png") {
        ImageIO.write(PrintPortrait.read(Path.of(selection.source)), "png", it.toFile())
    }

    override suspend fun copySource(path: String): String = createCopy("png") {
        ImageIO.write(PrintPortrait.read(resolve(path)), "png", it.toFile())
    }

    override suspend fun render(path: String, crop: PhotoCrop): String = createCopy("jpg") {
        PrintPortrait.write(resolve(path), it, crop)
    }

    private suspend fun createCopy(extension: String, write: (Path) -> Unit): String {
        var destination: Path? = null
        var transferred = false
        try {
            val path = withContext(Dispatchers.IO) {
                Files.createDirectories(photos)
                val temporary = Files.createTempFile(photos, "photo-", ".tmp")
                destination = photos.resolve("${UUID.randomUUID()}.$extension")
                try {
                    write(temporary)
                    Files.move(temporary, checkNotNull(destination), ATOMIC_MOVE)
                    "documents/resume-photos/${checkNotNull(destination).fileName}"
                } finally {
                    Files.deleteIfExists(temporary)
                }
            }
            currentCoroutineContext().ensureActive()
            transferred = true
            return path
        } finally {
            if (!transferred) {
                withContext(NonCancellable + Dispatchers.IO) {
                    destination?.let { Files.deleteIfExists(it) }
                }
            }
        }
    }

    override suspend fun read(path: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = resolve(path)
        if (Files.isRegularFile(file)) {
            val bytes = Files.readAllBytes(file)
            require(ImageIO.read(ByteArrayInputStream(bytes)) != null) { "Photo file is invalid" }
            bytes
        } else {
            null
        }
    }

    override suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(resolve(path))
        Unit
    }

    private fun resolve(path: String): Path {
        val file = root.resolve(path).normalize()
        require(!Path.of(path).isAbsolute && file.parent == photos) { "Invalid resume photo path" }
        return file
    }
}
