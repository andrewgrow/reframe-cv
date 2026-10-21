package reframecv.ui.components.projects.coverletters

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
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.importing.ImportedCoverLetterFile
import reframecv.ui.context.AppComponentContext

/** Reports import progress and errors without saving or discarding the current draft. */
enum class CoverLetterImportState { Idle, Importing, Imported, Failed }

/** Lets the creation page import fields and release temporary sources on close. */
interface CoverLetterImportComponent {
    val state: Value<CoverLetterImportState>
    fun onImport()
    fun onReset()
    fun onClose()
}

/** Owns imported temporary files for the lifetime of the cover-letter creation page. */
class DefaultCoverLetterImportComponent(
    componentContext: AppComponentContext,
    private val imported: (CoverLetter) -> Unit,
) : CoverLetterImportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val files = mutableListOf<ImportedCoverLetterFile>()
    private var closed = false
    override val state = MutableValue(CoverLetterImportState.Idle)

    init {
        componentContext.lifecycle.doOnDestroy(::onClose)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onImport() {
        if (closed || state.value == CoverLetterImportState.Importing) return
        state.value = CoverLetterImportState.Importing
        dependencies.eventHistory.record("CoverLetter Import(create)")
        scope.launch {
            try {
                val file = dependencies.coverLetterFilesUseCases.importDocument()
                if (file == null) {
                    state.value = CoverLetterImportState.Idle
                } else {
                    files += file
                    imported(file.coverLetter)
                    state.value = CoverLetterImportState.Imported
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Import cover letter from JSON",
                    error,
                    mapOf("mode" to "create"),
                )
                state.value = CoverLetterImportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value !=
            CoverLetterImportState.Importing
        ) {
            state.value = CoverLetterImportState.Idle
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onClose() {
        if (closed) return
        closed = true
        scope.cancel()
        val pending = files.toList()
        files.clear()
        CoroutineScope(NonCancellable + Dispatchers.Main).launch {
            pending.forEach { file ->
                try {
                    file.deleteTemporaryFile()
                } catch (error: Exception) {
                    dependencies.logger.error(
                        "Delete imported cover letter temporary file",
                        error,
                        emptyMap(),
                    )
                }
            }
        }
    }
}
