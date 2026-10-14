package reframecv.repository

import java.awt.Color
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import reframecv.attachments.SelectedAttachment
import reframecv.database.resume.ResumeContentJson
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.logging.ApplicationLogger
import reframecv.photos.DesktopResumePhotoStorage
import reframecv.photos.ResumePhotoPicker
import reframecv.photos.photoBytes

class ResumePhotoPersistenceTest : WorkspaceDatabaseTest() {
    private val storage get() = DesktopResumePhotoStorage(temporaryFolder.root.toPath())
    private val photos get() = LocalResumePhotos(
        database.resumeDao(),
        storage,
        ResumePhotoPicker { null },
        ApplicationLogger.None,
    )

    @Test
    fun persistsPhotoOutsideContentAndRemovesOldFileOnlyAfterSuccessfulSave() = runBlocking {
        val manager = photos
        val repository = LocalResumesRepository(database.resumeDao(), manager)
        val first = prepare("first", Color.RED)
        val projectId = project()
        val id = repository.create(resume(projectId).copy(photoPath = first))
        database.close()
        openDatabase()
        val reopened = LocalResumesRepository(database.resumeDao(), photos)
        val record = reopened.observeResumes(projectId).first().single()
        assertEquals(first, record.photoPath)
        assertFalse(ResumeContentJson.encodeForExport(record.content).contains("\"photoPath\""))
        val replacement = prepare("second", Color.BLUE)
        assertNotNull(storage.read(first))
        assertFailsWith<IllegalArgumentException> {
            reopened.update(record.copy(photoPath = replacement, sourceResumeId = 999))
        }
        assertNotNull(storage.read(first))
        assertEquals(first, database.resumeDao().findById(id)?.entity?.photoPath)
        reopened.update(record.copy(photoPath = replacement))
        assertNull(storage.read(first))
        assertNotNull(storage.read(replacement))
        reopened.delete(id)
        assertNull(storage.read(replacement))
    }

    @Test
    fun draftDisposalPreservesReferencedPhotosAndProjectDeletionCleansSubtree() = runBlocking {
        val manager = photos
        val repository = LocalResumesRepository(database.resumeDao(), manager)
        val root = project()
        val child = LocalProjectsRepository(database.projectDao()).createProject("Child", root).id
        val path = prepare("shared", Color.GREEN)
        val first = repository.create(resume(child).copy(photoPath = path))
        val second = repository.create(resume(child).copy(photoPath = path))
        manager.discard(path)
        assertNotNull(storage.read(path))
        repository.delete(first)
        assertNotNull(storage.read(path))
        val draft = prepare("draft", Color.BLUE)
        manager.discard(draft)
        assertNull(storage.read(draft))
        LocalProjectsRepository(database.projectDao(), photos = manager).deleteProject(root)
        assertEquals(null, database.resumeDao().findById(second))
        assertNull(storage.read(path))
    }

    private suspend fun prepare(name: String, color: Color): String {
        val source = temporaryFolder.newFile("$name.png").toPath()
        val original = photoBytes(color)
        Files.write(source, original)
        val path = storage.prepare(SelectedAttachment(source.toString(), "$name.png"))
        assertContentEquals(original, Files.readAllBytes(source))
        return path
    }
}
