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
import reframecv.export.DocumentExportResult
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class CoverLetterPdfWorkflowTest : ComponentTest() {
    private val record = CoverLetter(1, 7, "Letter", 0, 0, content = "Dear team")

    @Test
    fun exportSavesCurrentTextThenDefaultsToPdfAndBlocksDuplicateDownloads() = runBlocking<Unit> {
        val entered = CompletableDeferred<CoverLetter>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        var closes = 0
        dependencies.pdfExport = {
            calls++
            assertEquals(it, dependencies.coverLettersRepository.records.value.single())
            entered.complete(it)
            release.await()
            DocumentExportResult.Saved
        }
        val component = create { closes++ }
        val opened = watch(component) { it.exportVisible && !it.busy }
        runOnUiThread {
            component.onChange(record.copy(content = "Current printable text"))
            component.onExport()
        }
        withTimeout(5000) { opened.await() }
        val completed = watch(component) { it.notice == LetterNotice.PdfSaved && !it.exportVisible }
        runOnUiThread {
            assertEquals(CoverLetterExportFormat.Pdf, component.exportComponent.format.value)
            component.exportComponent.onDownload()
            component.exportComponent.onDownload()
            component.exportComponent.onFormatChange(CoverLetterExportFormat.Json)
            component.onClose()
            assertTrue(component.state.value.busy)
            assertEquals(CoverLetterExportFormat.Pdf, component.exportComponent.format.value)
        }
        assertEquals("Current printable text", withTimeout(5000) { entered.await() }.content)
        release.complete(Unit)
        withTimeout(5000) { completed.await() }
        assertEquals(1, calls)
        assertEquals(0, closes)
        assertFalse(component.state.value.busy)
    }

    @Test
    fun failureStaysInDialogAndRetryCancellationClosesWithoutPdfSuccess() = runBlocking<Unit> {
        dependencies.pdfExport = { error("Disk unavailable") }
        val component = create()
        val opened = watch(component) { it.exportVisible && !it.busy }
        runOnUiThread { component.onExport() }
        withTimeout(5000) { opened.await() }
        runOnUiThread { component.exportComponent.onDownload() }
        val logged = withTimeout(5000) { dependencies.logger.failures.receive() }
        assertEquals("Export cover letter as PDF", logged.operation)
        runOnUiThread {
            assertTrue(component.state.value.exportVisible)
            assertEquals(CoverLetterExportState.Failed, component.exportComponent.state.value)
        }
        dependencies.pdfExport = { DocumentExportResult.Cancelled }
        val closed = watch(component) { !it.exportVisible }
        runOnUiThread { component.exportComponent.onDownload() }
        withTimeout(5000) { closed.await() }
        assertEquals(LetterNotice.Saved, component.state.value.notice)
    }

    private fun create(close: () -> Unit = {}) = runOnUiThread {
        dependencies.coverLettersRepository.records.value = listOf(record)
        lifecycle.resume()
        DefaultCoverLetterEditorComponent(appComponentContext(), record, close, {})
    }

    private fun watch(
        component: CoverLetterEditorComponent,
        matches: (CoverLetterEditorState) -> Boolean,
    ) = runOnUiThread {
        CompletableDeferred<Unit>().also { done ->
            component.state.subscribe { if (matches(it)) done.complete(Unit) }
        }
    }
}
