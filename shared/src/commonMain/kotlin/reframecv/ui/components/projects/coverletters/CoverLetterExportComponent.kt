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
import reframecv.ui.context.AppComponentContext

/** Keeps export progress and retryable errors in the export dialog. */
enum class CoverLetterExportState { Idle, Exporting, Failed }

/** Lets the export dialog request a download and observe its progress. */
interface CoverLetterExportComponent {
    val state: Value<CoverLetterExportState>
    fun onDownload()
    fun onReset()
}

/** Runs document generation and file selection without blocking or losing the saved cover letter. */
class DefaultCoverLetterExportComponent(
    componentContext: AppComponentContext,
    private val coverLetter: () -> CoverLetter?,
    private val finished: () -> Unit,
) : CoverLetterExportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(CoverLetterExportState.Idle)

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        if (state.value == CoverLetterExportState.Exporting) return
        val record = coverLetter() ?: return
        state.value = CoverLetterExportState.Exporting
        dependencies.eventHistory.record("CoverLetter Export(coverLetterId=${record.id})")
        scope.launch {
            try {
                dependencies.coverLetterFilesUseCases.export(record)
                state.value = CoverLetterExportState.Idle
                finished()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Export cover letter as JSON",
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
        }
    }
}
