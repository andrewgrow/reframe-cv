package reframecv.importing

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import reframecv.domain.models.resume.content.ResumeContent

/** Parses a temporary copy and transfers its cleanup to the editor only after successful import. */
class DesktopResumeImporter(
    private val picker: JsonImportPicker = DesktopJsonImportPicker("Import resume"),
    private val temporaryDirectory: Path? = null,
) : ResumeImporter {
    override suspend fun pickAndImport(): ImportedResumeFile? {
        val source = picker.pick() ?: return null
        var temporary: Path? = null
        var transferred = false
        try {
            val content = withContext(Dispatchers.IO) {
                temporary = if (temporaryDirectory == null) {
                    Files.createTempFile("reframecv-resume-import-", ".json")
                } else {
                    Files.createTempFile(temporaryDirectory, "reframecv-resume-import-", ".json")
                }
                val copy = checkNotNull(temporary)
                Files.copy(Path.of(source), copy, StandardCopyOption.REPLACE_EXISTING)
                ResumeImportJson.decode(Files.readString(copy))
            }
            currentCoroutineContext().ensureActive()
            val imported = DesktopImportedResumeFile(content, checkNotNull(temporary))
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
private class DesktopImportedResumeFile(
    override val content: ResumeContent,
    private val path: Path,
) : ImportedResumeFile {
    override suspend fun deleteTemporaryFile() = withContext(NonCancellable + Dispatchers.IO) {
        Files.deleteIfExists(path)
        Unit
    }
}
