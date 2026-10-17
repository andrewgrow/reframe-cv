package reframecv.importing

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import reframecv.domain.models.vacancy.Vacancy

/** Parses a temporary copy and transfers its cleanup to the editor only after successful import. */
class DesktopVacancyImporter(
    private val picker: JsonImportPicker = DesktopJsonImportPicker("Import vacancy"),
    private val temporaryDirectory: Path? = null,
) : VacancyImporter {
    override suspend fun pickAndImport(): ImportedVacancyFile? {
        val source = picker.pick() ?: return null
        var temporary: Path? = null
        var transferred = false
        try {
            val content = withContext(Dispatchers.IO) {
                temporary = if (temporaryDirectory == null) {
                    Files.createTempFile("reframecv-vacancy-import-", ".json")
                } else {
                    Files.createTempFile(temporaryDirectory, "reframecv-vacancy-import-", ".json")
                }
                val copy = checkNotNull(temporary)
                Files.copy(Path.of(source), copy, StandardCopyOption.REPLACE_EXISTING)
                VacancyImportJson.decode(Files.readString(copy))
            }
            currentCoroutineContext().ensureActive()
            val imported = DesktopImportedVacancyFile(content, checkNotNull(temporary))
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
private class DesktopImportedVacancyFile(override val vacancy: Vacancy, private val path: Path) :
    ImportedVacancyFile {
    override suspend fun deleteTemporaryFile() = withContext(NonCancellable + Dispatchers.IO) {
        Files.deleteIfExists(path)
        Unit
    }
}
