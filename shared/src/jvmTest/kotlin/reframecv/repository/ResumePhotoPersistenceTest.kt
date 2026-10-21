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
import reframecv.database.workspace.WorkspaceDatabaseTest
import reframecv.logging.ApplicationLogger
import reframecv.photos.DesktopResumePhotoStorage
import reframecv.photos.PhotoCrop
import reframecv.photos.ResumePhotoPicker
import reframecv.photos.photoBytes
import reframecv.repository.MainResumeChange
import reframecv.serialization.ResumeContentJson

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
        val created = repository.createSaved(
            resume(projectId).copy(photoPath = first, photoVisible = false),
            MainResumeChange.Keep,
        )
        val id = created.id
        database.close()
        openDatabase()
        val reopened = LocalResumesRepository(database.resumeDao(), photos)
        val record = reopened.observeResumes(projectId).first().single()
        assertEquals(created.createdAt, record.createdAt)
        assertEquals(created.updatedAt, record.updatedAt)
        assertEquals(first, record.photoPath)
        assertFalse(record.photoVisible)
        reopened.update(record.copy(photoVisible = true))
        assertNotNull(storage.read(first))
        assertEquals(true, reopened.observeResumes(projectId).first().single().photoVisible)
        assertFalse(ResumeContentJson.encodeForExport(record.content).contains("photoVisible"))
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

    @Test
    fun bulkDeletionRemovesOnlyUnreferencedPhotosAndPreservesFilesOnFailure() = runBlocking<Unit> {
        val repository = LocalResumesRepository(database.resumeDao(), photos)
        val project = project()
        val shared = prepare("bulk-shared", Color.GREEN)
        val unique = prepare("bulk-unique", Color.RED)
        val first = repository.create(resume(project).copy(photoPath = shared))
        val second = repository.create(resume(project).copy(photoPath = unique))
        val retained = repository.create(resume(project).copy(photoPath = shared))
        assertFailsWith<IllegalArgumentException> {
            repository.deleteMany(project, setOf(first, -1))
        }
        assertNotNull(storage.read(shared))
        repository.deleteMany(project, setOf(first, second))
        assertNotNull(storage.read(shared))
        assertNull(storage.read(unique))
        repository.deleteMany(project, setOf(retained))
        assertNull(storage.read(shared))
    }

    private suspend fun prepare(name: String, color: Color): String {
        val source = temporaryFolder.newFile("$name.png").toPath()
        val original = photoBytes(color)
        Files.write(source, original)
        val draft = storage.prepareSource(SelectedAttachment(source.toString(), "$name.png"))
        val path = storage.render(draft, PhotoCrop())
        storage.delete(draft)
        assertContentEquals(original, Files.readAllBytes(source))
        return path
    }
}
