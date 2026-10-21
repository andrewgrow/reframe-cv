package reframecv.ui.components.projects.coverletters

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class CoverLetterEditorTest : ComponentTest() {
    @Test
    fun savesWithoutClosingAndCloseDiscardsLaterEdits() = runBlocking<Unit> {
        val record = CoverLetter(1, 7, "Letter", 0, 0, content = "Original")
        dependencies.coverLettersRepository.records.value = listOf(record)
        var closes = 0
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultCoverLetterEditorComponent(appComponentContext(), record, { closes++ }, {})
        }
        val saved = CompletableDeferred<Unit>()
        runOnUiThread {
            component.state.subscribe { if (it.notice == LetterNotice.Saved) saved.complete(Unit) }
            component.onChange(record.copy(name = "Updated", content = "Dear team"))
            component.onSave()
        }
        withTimeout(5000) { saved.await() }
        runOnUiThread {
            assertEquals(0, closes)
            assertEquals(
                "Dear team",
                dependencies.coverLettersRepository.records.value.single().content,
            )
            component.onChange(record.copy(content = "Unsaved"))
            component.onClose()
            assertEquals(1, closes)
            assertEquals(
                "Dear team",
                dependencies.coverLettersRepository.records.value.single().content,
            )
        }
    }

    @Test
    fun validatesNameAndRequiresConfirmationBeforePermanentDeletion() = runBlocking<Unit> {
        val record = CoverLetter(1, 7, "Letter", 0, 0)
        dependencies.coverLettersRepository.records.value = listOf(record)
        val deleted = CompletableDeferred<Long>()
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultCoverLetterEditorComponent(appComponentContext(), record, {
            }, { deleted.complete(it) })
        }
        runOnUiThread {
            component.onChange(record.copy(name = " "))
            component.onSave()
            assertEquals(LetterNotice.Validation, component.state.value.notice)
            component.onDelete()
            assertTrue(component.state.value.confirming)
            component.onClose()
            assertFalse(component.state.value.confirming)
            component.onDelete()
            assertEquals(1, dependencies.coverLettersRepository.records.value.size)
            component.onDelete()
        }
        assertEquals(1L, withTimeout(5000) { deleted.await() })
        assertEquals(emptyList(), dependencies.coverLettersRepository.records.value)
    }
}
