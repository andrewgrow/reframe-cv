package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class RunnerCopyTest : ComponentTest() {
    private suspend fun owner() = dependencies.providerSettingsUseCases.save(
        ProviderConnection(
            type = ApiProviderType.Apify,
            name = "Example source",
            configuration = ApifyConfiguration("fictional-token"),
        ),
    )
    private suspend fun source(owner: Long): Long = dependencies.connectionRunnersUseCases.save(
        RunnerDraft(
            name = "Example search",
            active = false,
            searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
            maxResults = "25",
        ).applyTo(RunnerDraft.newRunner(owner)),
    )
    private suspend fun editor(owner: Long) = runOnUiThread {
        lifecycle.resume()
        DefaultRunnerEditorComponent(appComponentContext(), RunnerDraft.newRunner(owner)) {}
    }
    private suspend fun <T : Any> await(value: Value<T>, check: (T) -> Boolean): T {
        val ready = CompletableDeferred<T>()
        val subscription = runOnUiThread { value.subscribe { if (check(it)) ready.complete(it) } }
        return try {
            withTimeout(5000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun copiedSettingsCanBeEditedAndSavedAsANewRunnerOnTheCurrentConnection() = runBlocking<Unit> {
        val sourceOwner = owner()
        val sourceId = source(sourceOwner)
        val targetOwner = owner()
        val editor = editor(targetOwner)
        runOnUiThread { editor.copy.open() }
        await(editor.copy.state) { !it.loading }
        runOnUiThread {
            editor.copy.select(sourceId)
            val copied = editor.copy.state.value.draft!!
            assertEquals("Example search (copy)", copied.name)
            assertEquals("25", copied.maxResults)
            assertFalse(copied.active)
            editor.copy.edit(copied.copy(name = "New search", maxResults = "50"))
            editor.copy.apply()
            assertFalse(editor.copy.state.value.visible)
            assertEquals(targetOwner, editor.state.value.runner.connectionId)
            assertEquals(0L, editor.state.value.runner.id)
            assertEquals("50", editor.state.value.draft.maxResults)
        }
        assertEquals(1, dependencies.connectionRunnersRepository.records.value.size)
        runOnUiThread { editor.onSave() }
        val saved = await(editor.state) { it.successfulSaves > 0 }
        assertTrue(saved.runner.id != sourceId)
        assertEquals(targetOwner, saved.runner.connectionId)
        val original = dependencies.connectionRunnersRepository.records.value.single {
            it.id ==
                sourceId
        }
        assertEquals("Example search", original.name)
        assertEquals(2, dependencies.connectionRunnersRepository.records.value.size)
        assertTrue(dependencies.connectionRunnersRepository.history.value.isEmpty())
        assertEquals(0, dependencies.runnerExecutionRepository.starts)
        runOnUiThread { editor.copy.open() }
        assertFalse(editor.copy.state.value.visible)
    }

    @Test
    fun cancelAndBackLeaveTheEditorsDraftUntouched() = runBlocking<Unit> {
        val owner = owner()
        val source = source(owner)
        val editor = editor(owner)
        val original = editor.state.value.draft.copy(name = "Unfinished search")
        runOnUiThread {
            editor.onDraft(original)
            editor.copy.open()
        }
        await(editor.copy.state) { !it.loading }
        runOnUiThread {
            editor.copy.select(source)
            editor.copy.edit(editor.copy.state.value.draft!!.copy(maxResults = "999"))
            editor.onSave()
            editor.onBack()
            assertFalse(editor.copy.state.value.visible)
            assertEquals(original, editor.state.value.draft)
            editor.copy.apply()
            assertEquals(original, editor.state.value.draft)
        }
        assertEquals(1, dependencies.connectionRunnersRepository.records.value.size)
    }

    @Test
    fun failedSourcesCanRetryAndMalformedConfigurationsAreSkipped() = runBlocking<Unit> {
        val owner = owner()
        val source = source(owner)
        val record = dependencies.connectionRunnersRepository.records.value.single()
        dependencies.connectionRunnersRepository.records.value +=
            record.copy(id = 99, configuration = "broken")
        dependencies.connectionRunnersRepository.failRead = true
        val editor = editor(owner)
        runOnUiThread { editor.copy.open() }
        await(editor.copy.state) { it.failed }
        dependencies.connectionRunnersRepository.failRead = false
        runOnUiThread { editor.copy.open() }
        val loaded = await(editor.copy.state) { !it.loading }
        assertFalse(loaded.failed)
        assertEquals(listOf(source), loaded.sources.map { it.runner.id })
        dependencies.connectionRunnersUseCases.delete(source)
        await(editor.copy.state) { it.sources.isEmpty() }
        runOnUiThread {
            editor.copy.select(source)
            assertEquals(null, editor.copy.state.value.draft)
            editor.copy.dismiss()
        }
    }
}
