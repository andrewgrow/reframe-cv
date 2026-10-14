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
import reframecv.database.resume.ResumeContentJson
import reframecv.domain.models.resume.content.sampleResumeContent

class DesktopResumeImporterTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun parsesIndependentTemporaryCopyAndDeletesItOnlyWhenReleased() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val source = folder.newFile("resume.json").toPath()
        val content = sampleResumeContent()
        val original = ResumeContentJson.encodeForExport(content)
        Files.writeString(source, original)
        val importer = DesktopResumeImporter(ResumeImportPicker { source.toString() }, temporary)
        val imported = checkNotNull(importer.pickAndImport())
        assertEquals(content, imported.content)
        val copy = Files.list(temporary).use { it.toList().single() }
        assertEquals(original, Files.readString(copy))
        Files.writeString(source, "changed by user")
        assertEquals(content, imported.content)
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
            DesktopResumeImporter(
                ResumeImportPicker {
                    null
                },
                temporary,
            ).pickAndImport(),
        )
        assertEmpty(temporary)
        val source = folder.newFile("invalid.json").toPath()
        Files.writeString(source, "invalid JSON")
        assertFailsWith<SerializationException> {
            DesktopResumeImporter(
                ResumeImportPicker {
                    source.toString()
                },
                temporary,
            ).pickAndImport()
        }
        assertEmpty(temporary)
        assertEquals("invalid JSON", Files.readString(source))
        assertFailsWith<java.nio.file.NoSuchFileException> {
            DesktopResumeImporter(
                ResumeImportPicker { source.resolveSibling("missing.json").toString() },
                temporary,
            ).pickAndImport()
        }
        assertEmpty(temporary)
    }

    @Test
    fun lifecycleCancellationDuringPickerDoesNotLeaveTemporaryFiles() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val importer = DesktopResumeImporter(
            ResumeImportPicker {
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
