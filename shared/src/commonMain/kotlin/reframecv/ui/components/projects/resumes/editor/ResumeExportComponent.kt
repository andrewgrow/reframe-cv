package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.resume.Resume
import reframecv.ui.context.AppComponentContext

/** Keeps export progress and retryable errors in the export dialog. */
enum class ResumeExportState { Idle, Exporting, Failed }

/** Selects the printable document or portable resume data. */
enum class ResumeExportFormat { Pdf, Json }

/** Lets the export dialog request a download and observe its progress. */
interface ResumeExportComponent {
    val state: Value<ResumeExportState>
    val format: Value<ResumeExportFormat>
    fun onFormatChange(format: ResumeExportFormat)
    fun onDownload()
    fun onReset()
}

/** Runs document generation and file selection without blocking or losing the saved resume. */
class DefaultResumeExportComponent(
    componentContext: AppComponentContext,
    private val resume: () -> Resume?,
    private val finished: () -> Unit,
) : ResumeExportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(ResumeExportState.Idle)
    override val format = MutableValue(ResumeExportFormat.Pdf)

    override fun onFormatChange(format: ResumeExportFormat) {
        if (state.value != ResumeExportState.Exporting) {
            this.format.value = format
            state.value = ResumeExportState.Idle
        }
    }

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        if (state.value == ResumeExportState.Exporting) return
        val record = resume() ?: return
        val selected = format.value
        state.value = ResumeExportState.Exporting
        dependencies.eventHistory.record("Resume Export(resumeId=${record.id})")
        scope.launch {
            try {
                when (selected) {
                    ResumeExportFormat.Pdf -> dependencies.resumeFilesUseCases.generatePdf(record)
                    ResumeExportFormat.Json -> dependencies.resumeFilesUseCases.export(record)
                }
                state.value = ResumeExportState.Idle
                finished()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Export resume as ${selected.name.uppercase()}",
                    error,
                    mapOf("projectId" to record.projectId, "resumeId" to record.id),
                )
                state.value = ResumeExportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value != ResumeExportState.Exporting) {
            state.value = ResumeExportState.Idle
            format.value = ResumeExportFormat.Pdf
        }
    }
}
