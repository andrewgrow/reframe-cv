package reframecv.recognition

import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Prepares PDF bytes from selected or managed files and verifies saved snapshot integrity. */
class DesktopVacancyDocumentReader(directory: Path) : VacancyDocumentReader {
    private val root = directory.toAbsolutePath().normalize()

    override suspend fun read(source: RecognitionSource): RecognitionDocument =
        withContext(Dispatchers.IO) {
            val name = when (source) {
                is RecognitionSource.Selected -> source.file.fileName
                is RecognitionSource.Saved -> source.attachment.fileName
            }
            if (!name.endsWith(".pdf", ignoreCase = true)) fail(RecognitionError.UnsupportedFile)
            val bytes = readBytes(source)
            if (source is RecognitionSource.Saved) {
                val hash = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
                if (source.attachment.fileSize != bytes.size.toLong() ||
                    source.attachment.sha256 != hash
                ) {
                    fail(RecognitionError.ChangedFile)
                }
            }
            if (!bytes.take(
                    PDF_HEADER.length,
                ).toByteArray().contentEquals(PDF_HEADER.encodeToByteArray())
            ) {
                fail(RecognitionError.InvalidPdf)
            }
            RecognitionDocument(name, bytes)
        }

    private fun readBytes(source: RecognitionSource): ByteArray = try {
        val path = when (source) {
            is RecognitionSource.Selected -> Path.of(source.file.source)

            is RecognitionSource.Saved -> {
                val relative = Path.of(source.attachment.filePath)
                val path = root.resolve(relative).normalize()
                if (relative.isAbsolute || path.parent != root.resolve("attachments")) {
                    fail(RecognitionError.UnreadableFile)
                }
                path
            }
        }
        if (!Files.isRegularFile(path)) fail(RecognitionError.UnreadableFile)
        Files.readAllBytes(path)
    } catch (_: InvalidPathException) {
        fail(RecognitionError.UnreadableFile)
    } catch (_: IOException) {
        fail(RecognitionError.UnreadableFile)
    }

    private fun fail(reason: RecognitionError): Nothing = throw RecognitionFileException(reason)

    companion object {
        private const val PDF_HEADER = "%PDF-"
    }
}
