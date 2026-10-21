package reframecv.ui.components.projects.resumes

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.importing.ImportedResumeFile
import reframecv.importing.ResumeImporter
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeImportState
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ResumeFilesUseCases

class ResumeImportComponentTest : ComponentTest() {
    @Test
    fun importsOnlyIntoDraftBlocksDuplicateActionsAndReleasesCopyOnClose() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val deleted = CompletableDeferred<Unit>()
        val loaded = CompletableDeferred<Unit>()
        val content = sampleResumeContent()
        val file = file(content, deleted)
        var calls = 0
        var saves = 0
        var closes = 0
        val component = editor(
            ResumeImporter {
                calls++
                entered.complete(Unit)
                release.await()
                file
            },
            save = { saves++ },
            close = { closes++ },
        )
        val original = ResumeDraft("Keep name", "Keep keywords", role = ResumeRole.Main)
            .addSection(ResumeSummarySection(text = "Replace"))
        runOnUiThread {
            component.onDraftChange(original)
            component.importComponent.state.subscribe {
                if (it == ResumeImportState.Imported) loaded.complete(Unit)
            }
            component.onImport()
        }
        withTimeout(10_000) {
            entered.await()
            runOnUiThread {
                assertTrue(component.isBusy)
                component.onImport()
                component.onSave()
                component.onClose()
                component.onDraftChange(ResumeDraft("Blocked"))
                assertEquals(original, component.draft.value)
            }
            assertEquals(1, calls)
            assertEquals(0, saves)
            assertEquals(0, closes)
            release.complete(Unit)
            loaded.await()
            runOnUiThread {
                val draft = component.draft.value
                assertEquals(original.withImportedContent(content), draft)
                assertEquals(content, draft.toResume(1).content)
                assertFalse(component.isBusy)
                assertFalse(deleted.isCompleted)
                component.onClose()
            }
            deleted.await()
            assertEquals(1, closes)
            assertEquals(0, saves)
        }
    }

    @Test
    fun saveIsExplicitAndLifecycleDestructionReleasesAllImportedCopies() = runBlocking {
        val deleted = CompletableDeferred<Unit>()
        val loaded = CompletableDeferred<Unit>()
        val content = sampleResumeContent()
        var saved: ResumeDraft? = null
        val component = editor(ResumeImporter { file(content, deleted) }, save = { saved = it })
        runOnUiThread {
            component.onDraftChange(ResumeDraft("Mobile"))
            component.importComponent.state.subscribe {
                if (it == ResumeImportState.Imported) loaded.complete(Unit)
            }
            component.onImport()
        }
        withTimeout(10_000) {
            loaded.await()
            assertEquals(null, saved)
            runOnUiThread {
                component.onSave()
                assertEquals(content, checkNotNull(saved).toResume(1).content)
                lifecycle.destroy()
            }
            deleted.await()
        }
    }

    @Test
    fun failedImportPreservesDraftAndRetryCancellationDoesNotSaveOrClose() = runBlocking {
        val failure = IllegalArgumentException("Invalid JSON")
        var fail = true
        val cancelled = CompletableDeferred<Unit>()
        val component = editor(
            ResumeImporter {
                if (fail) throw failure
                cancelled.complete(Unit)
                null
            },
        )
        val original = ResumeDraft("Keep")
        runOnUiThread {
            component.onDraftChange(original)
            component.onImport()
        }
        withTimeout(10_000) {
            val logged = dependencies.logger.failures.receive()
            assertSame(failure, logged.error)
            assertEquals("Import resume from JSON", logged.operation)
            runOnUiThread {
                assertEquals(ResumeImportState.Failed, component.importComponent.state.value)
                assertEquals(original, component.draft.value)
                fail = false
                component.onImport()
            }
            cancelled.await()
            runOnUiThread {
                assertEquals(ResumeImportState.Idle, component.importComponent.state.value)
                assertEquals(original, component.draft.value)
            }
        }
    }

    @Test
    fun destructionDuringImportCancelsTheOperationAndNeverChangesTheDraft() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val cleaned = CompletableDeferred<Unit>()
        val component = editor(
            ResumeImporter {
                entered.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cleaned.complete(Unit)
                }
            },
        )
        val original = ResumeDraft("Keep")
        runOnUiThread {
            component.onDraftChange(original)
            component.onImport()
        }
        withTimeout(10_000) {
            entered.await()
            runOnUiThread { lifecycle.destroy() }
            cleaned.await()
            assertEquals(original, component.draft.value)
        }
    }

    @Test
    fun repeatedImportsKeepEachCopyUntilCloseAndCleanupRunsOnce() = runBlocking {
        val imported = listOf(CompletableDeferred<Unit>(), CompletableDeferred<Unit>())
        val deleted = listOf(CompletableDeferred<Unit>(), CompletableDeferred<Unit>())
        var calls = 0
        var completions = 0
        val component = editor(
            ResumeImporter {
                val index = calls++
                file(ResumeContent(language = if (index == 0) "EN" else "DE"), deleted[index])
            },
        )
        runOnUiThread {
            component.importComponent.state.subscribe {
                if (it == ResumeImportState.Imported) imported[completions++].complete(Unit)
            }
            component.onImport()
        }
        withTimeout(10_000) {
            imported[0].await()
            runOnUiThread { component.onImport() }
            imported[1].await()
            runOnUiThread {
                assertEquals("DE", component.draft.value.language)
                assertTrue(deleted.none { it.isCompleted })
                component.onClose()
                lifecycle.destroy()
            }
            deleted.forEach { it.await() }
            assertEquals(2, calls)
        }
    }

    private fun file(content: ResumeContent, deleted: CompletableDeferred<Unit>) =
        object : ImportedResumeFile {
            override val content = content
            override suspend fun deleteTemporaryFile() {
                deleted.complete(Unit)
            }
        }

    private fun editor(
        importer: ResumeImporter,
        save: (ResumeDraft) -> Unit = {},
        close: () -> Unit = {},
    ) = runOnUiThread {
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
                    override val resumeImporter = importer
                },
            ),
            save,
            close,
        )
    }
}
