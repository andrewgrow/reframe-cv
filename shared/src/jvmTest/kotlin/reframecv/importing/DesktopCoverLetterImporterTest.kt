package reframecv.importing

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.serialization.CoverLetterContentJson

class DesktopCoverLetterImporterTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun parsesIndependentTemporaryCopyAndDeletesItOnlyWhenReleased() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val source = folder.newFile("coverLetter.json").toPath()
        val content = CoverLetter(projectId = 0, name = "Développeur", createdAt = 0, updatedAt = 0)
        val original = CoverLetterContentJson.encode(content)
        Files.writeString(source, original)
        val importer = DesktopCoverLetterImporter(JsonImportPicker { source.toString() }, temporary)
        val imported = checkNotNull(importer.pickAndImport())
        assertEquals(content, imported.coverLetter)
        val copy = Files.list(temporary).use { it.toList().single() }
        assertEquals(original, Files.readString(copy))
        Files.writeString(source, "changed by user")
        assertEquals(content, imported.coverLetter)
        assertEquals(original, Files.readString(copy))
        imported.deleteTemporaryFile()
        imported.deleteTemporaryFile()
        assertEmpty(temporary)
        assertEquals("changed by user", Files.readString(source))
    }

    @Test
    fun cancelledPickerCreatesNoCopyAndParseOrReadFailureRemovesItsCopy() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        assertEquals(
            null,
            DesktopCoverLetterImporter(
                JsonImportPicker {
                    null
                },
                temporary,
            ).pickAndImport(),
        )
        assertEmpty(temporary)
        val source = folder.newFile("invalid.json").toPath()
        Files.writeString(source, "invalid JSON")
        assertFailsWith<SerializationException> {
            DesktopCoverLetterImporter(
                JsonImportPicker {
                    source.toString()
                },
                temporary,
            ).pickAndImport()
        }
        assertEmpty(temporary)
        assertEquals("invalid JSON", Files.readString(source))
        assertFailsWith<java.nio.file.NoSuchFileException> {
            DesktopCoverLetterImporter(
                JsonImportPicker { source.resolveSibling("missing.json").toString() },
                temporary,
            ).pickAndImport()
        }
        assertEmpty(temporary)
    }

    @Test
    fun lifecycleCancellationDuringPickerDoesNotLeaveTemporaryFiles() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val importer = DesktopCoverLetterImporter(
            JsonImportPicker {
                entered.complete(Unit)
                awaitCancellation()
            },
            temporary,
        )
        val job = async { importer.pickAndImport() }
        entered.await()
        job.cancelAndJoin()
        assertEmpty(temporary)
    }

    private fun assertEmpty(directory: Path) {
        assertTrue(Files.list(directory).use { it.toList().isEmpty() })
    }
}
