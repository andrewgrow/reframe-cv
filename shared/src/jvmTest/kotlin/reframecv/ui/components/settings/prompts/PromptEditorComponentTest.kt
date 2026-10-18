package reframecv.ui.components.settings.prompts

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class PromptEditorComponentTest : ComponentTest() {
    private fun editor(prompt: Prompt = Prompt(), back: () -> Unit = {}) = runOnUiThread {
        lifecycle.resume()
        DefaultPromptEditorComponent(appComponentContext(), prompt, back)
    }

    @Test
    fun unnamedPromptsStayOpenAndRepeatedSavesKeepTheirIdentity() = runBlocking {
        var closed = false
        val editor = editor(back = { closed = true })
        runOnUiThread {
            editor.onText(" First instructions ")
            editor.onComponent(PromptComponent.Vacancies)
            editor.onSave()
        }
        await(editor) { it.successfulSaves == 1L }
        val id = runOnUiThread { editor.state.value.prompt.id }
        runOnUiThread {
            assertFalse(closed)
            assertEquals("", editor.state.value.prompt.title)
            assertEquals("First instructions", editor.state.value.prompt.text)
            editor.onText("New instructions")
            editor.onActive(false)
            editor.onSave()
        }
        await(editor) { it.successfulSaves == 2L }
        assertEquals(1, dependencies.promptsRepository.records.value.size)
        val saved = dependencies.promptsRepository.records.value.last()
        assertEquals(id, saved.id)
        assertFalse(saved.active)
        assertEquals(PromptComponent.Vacancies, saved.component)
    }

    @Test
    fun closeDiscardsUnsavedChangesIncludingStatus() {
        val original = Prompt(id = 1, text = "Saved instructions")
        dependencies.promptsRepository.records.value = listOf(original)
        var closed = false
        val editor = editor(original) { closed = true }
        runOnUiThread {
            editor.onText("Unsaved")
            editor.onActive(false)
            editor.onBack()
        }
        assertTrue(closed)
        assertEquals(original, dependencies.promptsRepository.records.value.single())
    }

    @Test
    fun validationAndWriteFailuresKeepTheDraftForRetry() = runBlocking {
        val editor = editor()
        runOnUiThread { editor.onSave() }
        assertEquals(PromptEditorError.Validation, editor.state.value.error)
        dependencies.promptsRepository.failSave = true
        runOnUiThread {
            editor.onText("Keep this text")
            editor.onSave()
        }
        await(editor) { it.error == PromptEditorError.Save }
        assertEquals("Keep this text", editor.state.value.prompt.text)
        dependencies.promptsRepository.failSave = false
        runOnUiThread { editor.onSave() }
        await(editor) { it.successfulSaves == 1L }
        assertEquals("Keep this text", dependencies.promptsRepository.records.value.last().text)
    }

    private suspend fun await(
        editor: PromptEditorComponent,
        ready: (PromptEditorState) -> Boolean,
    ) {
        val completed = CompletableDeferred<Unit>()
        val subscription =
            runOnUiThread { editor.state.subscribe { if (ready(it)) completed.complete(Unit) } }
        try {
            withTimeout(10_000) { completed.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }
}
