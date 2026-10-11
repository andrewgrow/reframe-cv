package reframecv.attachments

import java.awt.Desktop
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Keeps source-file copies inside the application's attachment directory. */
class FileAttachmentStorage(private val directory: Path) : AttachmentStorage {
    private val root = directory.toAbsolutePath().normalize()
    private val files = root.resolve("attachments")

    override suspend fun store(selection: SelectedAttachment): StoredAttachment =
        withContext(NonCancellable + Dispatchers.IO) {
            val extension = selection.fileName.substringAfterLast('.', "").lowercase()
            val mediaType = checkNotNull(MEDIA_TYPES[extension]) { "Unsupported snapshot format" }
            val source = Path.of(selection.source)
            require(Files.isRegularFile(source)) { "Source file is missing" }
            Files.createDirectories(files)
            val temporary = Files.createTempFile(files, "copy-", ".tmp")
            val destination = files.resolve("${UUID.randomUUID()}.$extension")
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                DigestInputStream(Files.newInputStream(source), digest).use { input ->
                    Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
                val size = Files.size(temporary)
                Files.move(temporary, destination, ATOMIC_MOVE)
                StoredAttachment(
                    "attachments/${destination.fileName}",
                    selection.fileName,
                    mediaType,
                    size,
                    digest.digest().toHexString(),
                )
            } finally {
                Files.deleteIfExists(temporary)
            }
        }

    override suspend fun status(attachment: StoredAttachment): AttachmentStatus =
        withContext(Dispatchers.IO) {
            val path = resolve(attachment.filePath)
            try {
                when {
                    !Files.isRegularFile(path) -> AttachmentStatus.Missing
                    Files.size(path) != attachment.fileSize -> AttachmentStatus.Changed
                    hash(path) != attachment.sha256 -> AttachmentStatus.Changed
                    else -> AttachmentStatus.Available
                }
            } catch (_: NoSuchFileException) {
                AttachmentStatus.Missing
            }
        }

    override suspend fun open(filePath: String) = withContext(Dispatchers.IO) {
        val path = resolve(filePath)
        if (!Files.isRegularFile(path)) throw IOException("Snapshot file is missing")
        check(
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN),
        ) {
            "Opening files is not supported"
        }
        Desktop.getDesktop().open(path.toFile())
    }

    override suspend fun delete(filePath: String) = withContext(Dispatchers.IO) {
        Files.deleteIfExists(resolve(filePath))
        Unit
    }

    override suspend fun cleanup(referencedPaths: Set<String>) = withContext(Dispatchers.IO) {
        if (Files.isDirectory(files)) {
            Files.list(files).use { paths ->
                paths.filter { Files.isRegularFile(it) }.forEach { path ->
                    if ("attachments/${path.fileName}" !in
                        referencedPaths
                    ) {
                        Files.deleteIfExists(path)
                    }
                }
            }
        }
    }

    private fun resolve(filePath: String): Path {
        val path = root.resolve(filePath).normalize()
        require(!Path.of(filePath).isAbsolute && path.parent == files) { "Invalid attachment path" }
        return path
    }

    private fun hash(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(Files.newInputStream(path), digest).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (input.read(buffer) != -1) { /* Read all original bytes. */ }
        }
        return digest.digest().toHexString()
    }

    companion object {
        private val MEDIA_TYPES = mapOf(
            "pdf" to "application/pdf",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "txt" to "text/plain",
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "json" to "application/json",
            "html" to "text/html",
            "htm" to "text/html",
        )
    }
}
