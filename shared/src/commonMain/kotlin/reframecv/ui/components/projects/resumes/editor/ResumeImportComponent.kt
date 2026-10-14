package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.resume.content.ResumeContent
import reframecv.importing.ImportedResumeFile
import reframecv.ui.context.AppComponentContext

/** Reports import progress and errors without saving or discarding the current draft. */
enum class ResumeImportState { Idle, Importing, Imported, Failed }

/** Lets the creation page import fields and release temporary sources on close. */
interface ResumeImportComponent {
    val state: Value<ResumeImportState>
    fun onImport()
    fun onReset()
    fun onClose()
}

/** Owns imported temporary files for the lifetime of the resume creation page. */
class DefaultResumeImportComponent(
    componentContext: AppComponentContext,
    private val imported: (ResumeContent) -> Unit,
) : ResumeImportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val files = mutableListOf<ImportedResumeFile>()
    private var closed = false
    override val state = MutableValue(ResumeImportState.Idle)

    init {
        componentContext.lifecycle.doOnDestroy(::onClose)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onImport() {
        if (closed || state.value == ResumeImportState.Importing) return
        state.value = ResumeImportState.Importing
        dependencies.eventHistory.record("Resume Import(create)")
        scope.launch {
            try {
                val file = dependencies.resumeImporter.pickAndImport()
                if (file == null) {
                    state.value = ResumeImportState.Idle
                } else {
                    files += file
                    imported(file.content)
                    state.value = ResumeImportState.Imported
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Import resume from JSON",
                    error,
                    mapOf("mode" to "create"),
                )
                state.value = ResumeImportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value != ResumeImportState.Importing) state.value = ResumeImportState.Idle
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onClose() {
        if (closed) return
        closed = true
        scope.cancel()
        val pending = files.toList()
        files.clear()
        scope.launch(NonCancellable) {
            pending.forEach { file ->
                try {
                    file.deleteTemporaryFile()
                } catch (error: Exception) {
                    dependencies.logger.error(
                        "Delete imported resume temporary file",
                        error,
                        emptyMap(),
                    )
                }
            }
        }
    }
}
