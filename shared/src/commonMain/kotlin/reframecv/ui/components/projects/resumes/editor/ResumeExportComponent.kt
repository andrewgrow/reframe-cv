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

/** Lets the export dialog request a download and observe its progress. */
interface ResumeExportComponent {
    val state: Value<ResumeExportState>
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

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        if (state.value == ResumeExportState.Exporting) return
        val record = resume() ?: return
        state.value = ResumeExportState.Exporting
        dependencies.eventHistory.record("Resume Export(resumeId=${record.id})")
        scope.launch {
            try {
                dependencies.resumeFilesUseCases.export(record)
                state.value = ResumeExportState.Idle
                finished()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Export resume as JSON",
                    error,
                    mapOf("projectId" to record.projectId, "resumeId" to record.id),
                )
                state.value = ResumeExportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value != ResumeExportState.Exporting) state.value = ResumeExportState.Idle
    }
}
