package reframecv.recognition

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.attachments.FileAttachmentStorage
import reframecv.attachments.SelectedAttachment
import reframecv.domain.models.vacancyattachment.VacancyAttachment

class DesktopVacancyDocumentReaderTest {
    @get:Rule val folder = TemporaryFolder()
    private val bytes = "%PDF-1.7\noriginal PDF bytes".encodeToByteArray()

    @Test
    fun preparesOriginalBytesWithoutCopyingOrChangingTheSelectedFile() = runBlocking {
        val file = folder.newFile("job.PDF").toPath()
        Files.write(file, bytes)
        val reader = DesktopVacancyDocumentReader(folder.root.toPath())
        val result = reader.read(
            RecognitionSource.Selected(SelectedAttachment(file.toString(), "job.PDF")),
        )
        assertEquals("job.PDF", result.fileName)
        assertContentEquals(bytes, result.bytes)
        assertContentEquals(bytes, Files.readAllBytes(file))
    }

    @Test
    fun readsSavedSnapshotAndRejectsChangedBytesEvenWhenFileSizeIsUnchanged() = runBlocking {
        val file = folder.newFile("job.pdf").toPath()
        Files.write(file, bytes)
        val root = folder.newFolder("managed").toPath()
        val stored = FileAttachmentStorage(
            root,
        ).store(SelectedAttachment(file.toString(), "job.pdf"))
        val attachment = VacancyAttachment(
            createdAt = 100,
            filePath = stored.filePath,
            fileName = stored.fileName,
            mediaType = stored.mediaType,
            fileSize = stored.fileSize,
            sha256 = stored.sha256,
        )
        val reader = DesktopVacancyDocumentReader(root)
        assertContentEquals(bytes, reader.read(RecognitionSource.Saved(attachment)).bytes)
        Files.write(root.resolve(stored.filePath), bytes.copyOf().also { it[it.lastIndex] = 0 })
        assertEquals(
            RecognitionError.ChangedFile,
            assertFailsWith<RecognitionFileException> {
                reader.read(RecognitionSource.Saved(attachment))
            }.reason,
        )
        Files.delete(root.resolve(stored.filePath))
        assertEquals(
            RecognitionError.UnreadableFile,
            assertFailsWith<RecognitionFileException> {
                reader.read(RecognitionSource.Saved(attachment))
            }.reason,
        )
    }

    @Test
    fun reportsUnsupportedMissingAndInvalidPdfsWithoutReadingOutsideManagedStorage() = runBlocking {
        val file = folder.newFile("text.pdf").toPath()
        Files.writeString(file, "This is not a PDF")
        val reader = DesktopVacancyDocumentReader(folder.root.toPath())
        val cases = listOf(
            SelectedAttachment(file.toString(), "image.png") to RecognitionError.UnsupportedFile,
            SelectedAttachment(file.toString(), "text.pdf") to RecognitionError.InvalidPdf,
            SelectedAttachment(file.resolveSibling("missing.pdf").toString(), "job.pdf") to
                RecognitionError.UnreadableFile,
        )
        cases.forEach { (selected, reason) ->
            assertEquals(
                reason,
                assertFailsWith<RecognitionFileException> {
                    reader.read(RecognitionSource.Selected(selected))
                }.reason,
            )
        }
        val unsafe =
            VacancyAttachment(
                createdAt = 0,
                filePath = "../text.pdf",
                fileName = "job.pdf",
                mediaType = "application/pdf",
                fileSize = 0,
                sha256 = "hash",
            )
        assertEquals(
            RecognitionError.UnreadableFile,
            assertFailsWith<RecognitionFileException> {
                reader.read(RecognitionSource.Saved(unsafe))
            }.reason,
        )
    }
}
