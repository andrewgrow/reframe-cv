package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.export.DocumentExportResult
import reframecv.ui.context.AppComponentContext

/** Keeps export progress and retryable errors in the export dialog. */
enum class CoverLetterExportState { Idle, Exporting, Failed, EmptyText }

/** Selects printable PDF or portable JSON for a saved letter. */
enum class CoverLetterExportFormat { Pdf, Json }

/** Lets the export dialog request a download and observe its progress. */
interface CoverLetterExportComponent {
    val state: Value<CoverLetterExportState>
    val format: Value<CoverLetterExportFormat>
    fun onFormatChange(format: CoverLetterExportFormat)
    fun onDownload()
    fun onReset()
}

/** Runs document generation and file selection without blocking or losing the saved cover letter. */
class DefaultCoverLetterExportComponent(
    componentContext: AppComponentContext,
    private val coverLetter: () -> CoverLetter?,
    private val finished: () -> Unit,
    private val exported: (CoverLetterExportFormat) -> Unit = {},
) : CoverLetterExportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(CoverLetterExportState.Idle)
    override val format = MutableValue(CoverLetterExportFormat.Pdf)

    override fun onFormatChange(format: CoverLetterExportFormat) {
        if (state.value != CoverLetterExportState.Exporting) {
            this.format.value = format
            state.value = CoverLetterExportState.Idle
        }
    }

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        val record = coverLetter()?.takeUnless {
            state.value == CoverLetterExportState.Exporting
        } ?: return
        val selected = format.value
        if (selected == CoverLetterExportFormat.Pdf && record.content.isBlank()) {
            state.value = CoverLetterExportState.EmptyText
            return
        }
        state.value = CoverLetterExportState.Exporting
        dependencies.eventHistory.record("CoverLetter Export(coverLetterId=${record.id})")
        scope.launch {
            try {
                val result = when (selected) {
                    CoverLetterExportFormat.Pdf ->
                        dependencies.coverLetterFilesUseCases.generatePdf(record)

                    CoverLetterExportFormat.Json -> dependencies.coverLetterFilesUseCases.export(
                        record,
                    )
                }
                if (result == DocumentExportResult.Saved) exported(selected)
                state.value = CoverLetterExportState.Idle
                finished()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Export cover letter as ${selected.name.uppercase()}",
                    error,
                    mapOf("projectId" to record.projectId, "coverLetterId" to record.id),
                )
                state.value = CoverLetterExportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value !=
            CoverLetterExportState.Exporting
        ) {
            state.value = CoverLetterExportState.Idle
            format.value = CoverLetterExportFormat.Pdf
        }
    }
}
