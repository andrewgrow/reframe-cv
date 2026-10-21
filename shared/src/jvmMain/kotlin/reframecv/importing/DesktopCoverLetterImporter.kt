package reframecv.importing

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import reframecv.domain.models.coverletter.CoverLetter

/** Parses a temporary copy and transfers its cleanup to the editor only after successful import. */
class DesktopCoverLetterImporter(
    private val picker: JsonImportPicker = DesktopJsonImportPicker("Import cover letter"),
    private val temporaryDirectory: Path? = null,
) : CoverLetterImporter {
    override suspend fun pickAndImport(): ImportedCoverLetterFile? {
        val source = picker.pick() ?: return null
        var temporary: Path? = null
        var transferred = false
        try {
            val content = withContext(Dispatchers.IO) {
                temporary = if (temporaryDirectory == null) {
                    Files.createTempFile("reframecv-cover-letter-import-", ".json")
                } else {
                    Files.createTempFile(
                        temporaryDirectory,
                        "reframecv-cover-letter-import-",
                        ".json",
                    )
                }
                val copy = checkNotNull(temporary)
                Files.copy(Path.of(source), copy, StandardCopyOption.REPLACE_EXISTING)
                CoverLetterImportJson.decode(Files.readString(copy))
            }
            currentCoroutineContext().ensureActive()
            val imported = DesktopImportedCoverLetterFile(content, checkNotNull(temporary))
            transferred = true
            return imported
        } finally {
            if (!transferred) {
                withContext(NonCancellable + Dispatchers.IO) {
                    temporary?.let { Files.deleteIfExists(it) }
                }
            }
        }
    }
}

/** Retains the temporary JSON source independently of the user's original file. */
private class DesktopImportedCoverLetterFile(
    override val coverLetter: CoverLetter,
    private val path: Path,
) : ImportedCoverLetterFile {
    override suspend fun deleteTemporaryFile() = withContext(NonCancellable + Dispatchers.IO) {
        Files.deleteIfExists(path)
        Unit
    }
}
