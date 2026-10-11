package reframecv.attachments

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileAttachmentStorageTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun copyPreservesBytesAndSurvivesRemovalOfOriginal() = runBlocking {
        val source = folder.newFile("Source.PDF").toPath()
        val bytes = byteArrayOf(0, 1, -1, 42)
        Files.write(source, bytes)
        val root = folder.newFolder("app").toPath()
        val storage = FileAttachmentStorage(root)
        val saved = storage.store(SelectedAttachment(source.toString(), "Source.PDF"))
        Files.delete(source)
        assertEquals("Source.PDF", saved.fileName)
        assertEquals("application/pdf", saved.mediaType)
        assertEquals(4L, saved.fileSize)
        assertEquals(64, saved.sha256.length)
        assertTrue(Files.readAllBytes(root.resolve(saved.filePath)).contentEquals(bytes))
        assertEquals(AttachmentStatus.Available, FileAttachmentStorage(root).status(saved))
    }

    @Test
    fun detectsSameSizeChangesAndMissingFiles() = runBlocking {
        val source = folder.newFile("source.txt").toPath()
        Files.writeString(source, "abc")
        val storage = FileAttachmentStorage(folder.root.toPath())
        val saved = storage.store(SelectedAttachment(source.toString(), "source.txt"))
        Files.writeString(folder.root.toPath().resolve(saved.filePath), "xyz")
        assertEquals(AttachmentStatus.Changed, storage.status(saved))
        storage.delete(saved.filePath)
        assertEquals(AttachmentStatus.Missing, storage.status(saved))
        assertTrue(Files.exists(source))
    }

    @Test
    fun cleanupOnlyRemovesUnreferencedCopies() = runBlocking {
        val source = folder.newFile("original.txt").toPath()
        val storage = FileAttachmentStorage(folder.root.toPath())
        val keep = storage.store(SelectedAttachment(source.toString(), "original.txt"))
        val orphan = storage.store(SelectedAttachment(source.toString(), "original.txt"))
        val temporary = folder.root.toPath().resolve("attachments/copy-abandoned.tmp")
        Files.writeString(temporary, "partial")
        storage.cleanup(setOf(keep.filePath))
        assertEquals(AttachmentStatus.Available, storage.status(keep))
        assertEquals(AttachmentStatus.Missing, storage.status(orphan))
        assertTrue(!Files.exists(temporary))
        assertTrue(Files.exists(source))
    }

    @Test
    fun rejectsUnsupportedFormatAndPathsOutsideStorage() = runBlocking {
        val source = folder.newFile("original.exe").toPath()
        val storage = FileAttachmentStorage(folder.root.toPath())
        assertFailsWith<IllegalStateException> {
            storage.store(SelectedAttachment(source.toString(), "original.exe"))
        }
        assertFailsWith<IllegalArgumentException> { storage.delete("attachments/../original.exe") }
        assertFailsWith<IllegalArgumentException> { storage.delete(source.toString()) }
        assertTrue(Files.exists(source))
    }
}
