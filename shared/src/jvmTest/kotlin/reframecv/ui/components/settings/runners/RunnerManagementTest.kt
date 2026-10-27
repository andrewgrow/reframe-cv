package reframecv.ui.components.settings.runners

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerConfigurationJson
import reframecv.testing.ComponentTest
import reframecv.ui.components.settings.DefaultProviderEditorComponent
import reframecv.ui.threading.runOnUiThread

class RunnerManagementTest : ComponentTest() {
    private val url = "https://www.linkedin.com/jobs/search/?keywords=kotlin"
    private suspend fun owner() = dependencies.providerSettingsUseCases.save(
        ProviderConnection(
            type = ApiProviderType.Apify,
            name = "Sources",
            configuration = ApifyConfiguration("token"),
        ),
    )
    private suspend fun editor(owner: Long, runnerId: Long = 0): RunnerEditorComponent =
        runOnUiThread {
            lifecycle.resume()
            val runner =
                dependencies.connectionRunnersRepository.records.value.find { it.id == runnerId }
                    ?: RunnerDraft.newRunner(owner)
            DefaultRunnerEditorComponent(appComponentContext(), runner) {}
        }
    private suspend fun await(
        component: RunnerEditorComponent,
        test: (RunnerEditorState) -> Boolean,
    ): RunnerEditorState {
        val changed = CompletableDeferred<RunnerEditorState>()
        val subscription =
            runOnUiThread { component.state.subscribe { if (test(it)) changed.complete(it) } }
        return try {
            withTimeout(5000) { changed.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun savingCreatesOneRecordAndSubsequentSavesUpdateItWithoutClosing() = runBlocking<Unit> {
        val owner = owner()
        val editor = editor(owner)
        runOnUiThread {
            editor.onDraft(
                RunnerDraft(
                    name = "  Android  ",
                    searchUrls = " $url \n\n$url\n",
                    maxResults = " 25 ",
                ),
            )
            editor.onSave()
        }
        val first = await(editor) { it.successfulSaves == 1L }
        assertTrue(first.runner.id > 0)
        assertEquals("Android", first.draft.name)
        assertEquals(
            listOf(url),
            (
                RunnerConfigurationJson.decode(first.runner) as?
                    reframecv.domain.models.providers.LinkedInJobSearchConfiguration
                )?.searchUrls,
        )
        runOnUiThread {
            editor.onDraft(first.draft.copy(active = false, maxResults = "50"))
            editor.onSave()
        }
        val second = await(editor) { it.successfulSaves == 2L }
        assertEquals(first.runner.id, second.runner.id)
        assertFalse(second.runner.active)
        assertEquals(1, dependencies.connectionRunnersRepository.records.value.size)
        assertTrue(dependencies.connectionRunnersRepository.history.value.isEmpty())
    }

    @Test
    fun invalidInputsAndStorageErrorsKeepDraftForCorrectionAndRetry() = runBlocking<Unit> {
        val owner = owner()
        val editor = editor(owner)
        runOnUiThread {
            editor.onSave()
            assertEquals(RunnerEditorError.Validation, editor.state.value.error)
            editor.onDraft(
                RunnerDraft(
                    searchUrls = "https://linkedin.com.evil.test/jobs/search/",
                    maxResults = "0",
                ),
            )
            editor.onSave()
            assertEquals(RunnerEditorError.Validation, editor.state.value.error)
            assertTrue(dependencies.connectionRunnersRepository.records.value.isEmpty())
            editor.onDraft(RunnerDraft(searchUrls = url))
            dependencies.connectionRunnersRepository.failSave = true
            editor.onSave()
        }
        val failure = await(editor) { it.error == RunnerEditorError.Save }
        assertEquals(url, failure.draft.searchUrls)
        runOnUiThread {
            dependencies.connectionRunnersRepository.failSave = false
            editor.onSave()
        }
        val saved = await(editor) { it.successfulSaves == 1L }
        runOnUiThread {
            editor.onDelete()
            editor.onBack()
            assertFalse(editor.state.value.confirming)
            editor.onDelete()
            dependencies.connectionRunnersRepository.failDelete = true
            editor.onConfirmDelete()
        }
        await(editor) { it.error == RunnerEditorError.Delete }
        assertEquals(
            saved.runner.id,
            dependencies.connectionRunnersRepository.records.value.single().id,
        )
        runOnUiThread {
            dependencies.connectionRunnersRepository.failDelete = false
            editor.onConfirmDelete()
        }
        await(editor) { !it.confirming }
        assertTrue(dependencies.connectionRunnersRepository.records.value.isEmpty())
    }

    @Test
    fun newConnectionMustBeSavedAndRunnerNavigationPreservesProviderDraft() = runBlocking<Unit> {
        val saved = CompletableDeferred<Unit>()
        val provider = runOnUiThread {
            lifecycle.resume()
            DefaultProviderEditorComponent(
                appComponentContext(),
                ProviderConnection(
                    type = ApiProviderType.Apify,
                    name = "",
                    configuration = ApifyConfiguration("token"),
                ),
                saved = { saved.complete(Unit) },
                back = {},
            ).also {
                assertNull(it.runners.value.child)
                it.onSave()
            }
        }
        withTimeout(5000) { saved.await() }
        runOnUiThread {
            provider.onName("Unsaved provider name")
            val runners = assertNotNull(provider.runners.value.child).instance
            runners.onAdd()
            val editor = assertNotNull(runners.editorSlot.value.child).instance
            editor.onDraft(RunnerDraft(name = "Discarded", searchUrls = url))
            provider.onBack()
            assertNull(runners.editorSlot.value.child)
            assertEquals("Unsaved provider name", provider.state.value.connection.name)
            assertTrue(dependencies.connectionRunnersRepository.records.value.isEmpty())
        }
    }

    @Test
    fun listIsConnectionScopedAndFailedReadsCanBeRetried() = runBlocking<Unit> {
        val first = owner()
        val second = owner()
        dependencies.connectionRunnersUseCases.save(
            RunnerDraft(searchUrls = url).applyTo(RunnerDraft.newRunner(first)),
        )
        val foreign = dependencies.connectionRunnersUseCases.save(
            RunnerDraft(searchUrls = url).applyTo(RunnerDraft.newRunner(second)),
        )
        dependencies.connectionRunnersRepository.failRead = true
        val states = Channel<ProviderRunnersState>(Channel.UNLIMITED)
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultProviderRunnersComponent(appComponentContext(), first).also {
                it.state.subscribe { state -> states.trySend(state) }
            }
        }
        withTimeout(5000) { while (!states.receive().failed) Unit }
        runOnUiThread {
            dependencies.connectionRunnersRepository.failRead = false
            component.onRetry()
        }
        val ready = withTimeout(5000) {
            var state = states.receive()
            while (state.loading || state.failed) state = states.receive()
            state
        }
        assertEquals(listOf(first), ready.runners.map { it.connectionId })
        runOnUiThread {
            component.onEdit(foreign)
            assertNull(component.editorSlot.value.child)
            component.onEdit(ready.runners.single().id)
            assertNotNull(component.editorSlot.value.child)
            component.onBack()
            assertNull(component.editorSlot.value.child)
        }
    }

    @Test
    fun savingBlocksBackAndDraftChangesUntilPersistenceCompletes() = runBlocking<Unit> {
        val owner = owner()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        dependencies.connectionRunnersRepository.beforeSave = {
            started.complete(Unit)
            release.await()
        }
        val provider = runOnUiThread {
            lifecycle.resume()
            DefaultProviderEditorComponent(
                appComponentContext(),
                ProviderConnection(
                    id = owner,
                    type = ApiProviderType.Apify,
                    name = "Source",
                    configuration = ApifyConfiguration("token"),
                ),
                saved = {},
                back = {},
            )
        }
        val child = runOnUiThread {
            val runners = assertNotNull(provider.runners.value.child).instance
            runners.onAdd()
            assertNotNull(runners.editorSlot.value.child).instance.also {
                it.onDraft(RunnerDraft(searchUrls = url))
                it.onSave()
            }
        }
        withTimeout(5000) { started.await() }
        runOnUiThread {
            assertTrue(provider.isBusy)
            provider.onBack()
            child.onDraft(RunnerDraft(name = "Ignored"))
            child.onDelete()
            child.onSave()
            assertNotNull(provider.runners.value.child?.instance?.editorSlot?.value?.child)
            assertEquals(url, child.state.value.draft.searchUrls)
            assertFalse(child.state.value.confirming)
        }
        release.complete(Unit)
        await(child) { it.successfulSaves == 1L }
        runOnUiThread { assertFalse(provider.isBusy) }
    }
}
