package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.resume.Resume
import reframecv.photos.PhotoCrop
import reframecv.photos.ResumePhotos
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.DefaultResumePhotoComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ResumeFilesUseCases

class ResumePhotoComponentTest : ComponentTest() {
    @Test
    fun cancelledDraftDiscardsOnlyPreparedCopiesAndSavedPhotoSurvives() = runBlocking {
        val photos = Photos()
        val changed = Channel<String?>(Channel.UNLIMITED)
        val component = component(photos, "saved", changed::trySend)
        withTimeout(10_000) {
            photos.reads.receive()
            runOnUiThread { component.onChoose() }
            assertEquals("new", changed.receive())
            runOnUiThread { lifecycle.destroy() }
            assertEquals("new", photos.discarded.receive())
            assertTrue(photos.discarded.tryReceive().isFailure)
        }
    }

    @Test
    fun confirmedRemovalChangesDraftButKeepsSavedFileAndEscapeCancelsConfirmation() = runBlocking {
        val photos = Photos()
        val changed = Channel<String?>(Channel.UNLIMITED)
        val component = component(photos, "saved", changed::trySend)
        withTimeout(10_000) {
            photos.reads.receive()
            runOnUiThread {
                component.onOpen()
                component.onRequestDelete()
                component.onClose()
                assertTrue(component.visible.value)
                assertFalse(component.confirmingDelete.value)
                assertEquals("saved", component.state.value.path)
                component.onRequestDelete()
                component.onConfirmDelete()
            }
            assertEquals(null, changed.receive())
            runOnUiThread { lifecycle.destroy() }
            assertTrue(photos.discarded.tryReceive().isFailure)
        }
    }

    @Test
    fun savedSelectionIsReleasedFromDraftOwnershipAndMissingPhotoRemainsReplaceable() =
        runBlocking {
            val photos = Photos(missing = "saved")
            val changed = Channel<String?>(Channel.UNLIMITED)
            val component = component(photos, "saved", changed::trySend)
            withTimeout(10_000) {
                photos.reads.receive()
                runOnUiThread {
                    assertEquals(null, component.state.value.bytes)
                    assertFalse(component.state.value.failed)
                    component.onChoose()
                }
                assertEquals("new", changed.receive())
                runOnUiThread {
                    component.onSaved()
                    lifecycle.destroy()
                }
                assertTrue(photos.discarded.tryReceive().isFailure)
            }
        }

    @Test
    fun pendingReplacementIsDiscardedAndBusyOrFailedSelectionPreservesDraft() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val photos = Photos()
        photos.selection = {
            entered.complete(Unit)
            release.await()
            "first"
        }
        val changed = Channel<String?>(Channel.UNLIMITED)
        val component = component(photos, null, changed::trySend)
        withTimeout(10_000) {
            runOnUiThread { component.onChoose() }
            entered.await()
            runOnUiThread {
                component.onChoose()
                component.onRequestDelete()
                assertTrue(component.state.value.loading)
                assertFalse(component.confirmingDelete.value)
            }
            release.complete(Unit)
            assertEquals("first", changed.receive())
            assertEquals(1, photos.selections)
            photos.selection = { "second" }
            runOnUiThread { component.onChoose() }
            assertEquals("second", changed.receive())
            assertEquals("first", photos.discarded.receive())
            photos.selection = { error("Cannot decode photo") }
            runOnUiThread { component.onChoose() }
            assertEquals("Prepare resume photo", dependencies.logger.failures.receive().operation)
            runOnUiThread {
                assertTrue(component.state.value.failed)
                assertEquals("second", component.state.value.path)
                lifecycle.destroy()
            }
            assertEquals("second", photos.discarded.receive())
        }
    }

    @Test
    fun editorSavesPhotoExplicitlyAndFailedSaveRetainsDraftOwnership() = runBlocking {
        val photos = Photos()
        val loaded = CompletableDeferred<Unit>()
        var saved: ResumeDraft? = null
        val writes = Channel<ResumeDraft>(Channel.UNLIMITED)
        val editor = runOnUiThread {
            lifecycle.resume()
            DefaultResumeEditorComponent(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    object : ApplicationDependencies by dependencies {
                        override val resumeFilesUseCases get() = ResumeFilesUseCases(
                            resumeImporter,
                            resumeExporter,
                            resumePhotos,
                            resumePdfExporter,
                        )
                        override val resumePhotos = photos
                    },
                ),
                save = {
                    saved = it
                    writes.trySend(it)
                },
                close = {},
                initialResume = Resume(
                    id = 1,
                    projectId = 1,
                    name = "Mobile",
                    createdAt = 0,
                    updatedAt = 0,
                ),
            ).also { component ->
                component.photoComponent.state.subscribe {
                    if (it.path == "new" && !it.loading) loaded.complete(Unit)
                }
                component.photoComponent.onChoose()
                component.onSave()
                assertTrue(component.isBusy)
                assertEquals(null, saved)
            }
        }
        withTimeout(10_000) {
            loaded.await()
            runOnUiThread {
                assertEquals(null, saved)
                editor.onSave()
            }
            assertEquals("new", writes.receive().photoPath)
            runOnUiThread {
                editor.onSaveCompleted(ResumeSaveState.Failed)
                assertEquals("new", editor.draft.value.photoPath)
                editor.onSave()
                editor.onSaveCompleted(ResumeSaveState.Idle)
                lifecycle.destroy()
            }
            assertTrue(photos.discarded.tryReceive().isFailure)
        }
    }

    @Test
    fun rendersOnlyOnSavePreservesFramingOnFailureAndRetainsSourceAfterSuccess() = runBlocking {
        val photos = Photos()
        val changed = Channel<String?>(Channel.UNLIMITED)
        val component = component(photos, null, changed::trySend)
        withTimeout(10_000) {
            runOnUiThread { component.onChoose() }
            assertEquals("new", changed.receive())
            runOnUiThread { component.onCropChange(PhotoCrop(2f, 0.5f, -0.5f)) }
            assertTrue(photos.renders.tryReceive().isFailure)
            photos.rendering = { _, _ -> error("Cannot write photo") }
            val failed = CompletableDeferred<Boolean>()
            runOnUiThread { component.prepareSave { failed.complete(it) } }
            assertFalse(failed.await())
            assertEquals("Render resume photo", dependencies.logger.failures.receive().operation)
            assertEquals("new" to PhotoCrop(2f, 0.5f, -0.5f), photos.renders.receive())
            photos.rendering = { _, _ -> "portrait" }
            val saved = CompletableDeferred<Boolean>()
            runOnUiThread { component.prepareSave { saved.complete(it) } }
            assertTrue(saved.await())
            assertEquals("portrait", changed.receive())
            photos.renders.receive()
            runOnUiThread {
                component.onSaved()
                component.onCropChange(PhotoCrop(3f))
            }
            photos.rendering = { _, _ -> "portrait-2" }
            val resaved = CompletableDeferred<Boolean>()
            runOnUiThread { component.prepareSave { resaved.complete(it) } }
            assertTrue(resaved.await())
            assertEquals("new" to PhotoCrop(3f), photos.renders.receive())
            runOnUiThread { lifecycle.destroy() }
            val removed = setOf(photos.discarded.receive(), photos.discarded.receive())
            assertEquals(setOf("new", "portrait-2"), removed)
        }
    }

    private fun component(photos: ResumePhotos, path: String?, changed: (String?) -> Unit) =
        runOnUiThread {
            lifecycle.resume()
            DefaultResumePhotoComponent(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    object : ApplicationDependencies by dependencies {
                        override val resumeFilesUseCases get() = ResumeFilesUseCases(
                            resumeImporter,
                            resumeExporter,
                            resumePhotos,
                            resumePdfExporter,
                        )
                        override val resumePhotos = photos
                    },
                ),
                path,
                changed,
            )
        }

    private class Photos(private val missing: String? = null) : ResumePhotos {
        val reads = Channel<String>(Channel.UNLIMITED)
        val discarded = Channel<String>(Channel.UNLIMITED)
        var selection: suspend () -> String? = { "new" }
        var selections = 0
        val renders = Channel<Pair<String, PhotoCrop>>(Channel.UNLIMITED)
        var rendering: suspend (String, PhotoCrop) -> String = { path, _ -> path }
        override suspend fun choose(): String? {
            selections++
            return selection()
        }
        override suspend fun copySource(path: String) = path
        override suspend fun render(path: String, crop: PhotoCrop): String {
            renders.send(path to crop)
            return rendering(path, crop)
        }
        override suspend fun read(path: String): ByteArray? {
            reads.send(path)
            return if (path == missing) null else byteArrayOf(1)
        }
        override suspend fun discard(path: String) {
            discarded.send(path)
        }
    }
}
