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

/** Distinguishes save feedback from failures in the letter editor. */
enum class LetterNotice {
    None,
    Saved,
    SaveFailed,
    DeleteFailed,
    Validation,
    PdfSaved,
}

/** Keeps letter edits local until Save and reserves feedback without moving actions. */
data class CoverLetterEditorState(
    val record: CoverLetter,
    val busy: Boolean = false,
    val confirming: Boolean = false,
    val notice: LetterNotice = LetterNotice.None,
    val exportVisible: Boolean = false,
)

/** Creates, imports, edits, exports, and deletes a reusable project letter. */
interface CoverLetterEditorComponent {
    val importComponent: CoverLetterImportComponent
    val exportComponent: CoverLetterExportComponent
    fun onImport()
    fun onExport()
    fun onCloseExport()
    val state: Value<CoverLetterEditorState>
    fun onChange(record: CoverLetter)
    fun onSave()
    fun onDelete()
    fun onCancelDelete()
    fun onClose()
}

/** Saves letter drafts without leaving the page and confirms destructive deletion. */
class DefaultCoverLetterEditorComponent(
    private val context: AppComponentContext,
    record: CoverLetter,
    private val close: () -> Unit,
    private val deleted: (Long) -> Unit,
) : CoverLetterEditorComponent {
    override val state = MutableValue(CoverLetterEditorState(record))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val notices = CoverLetterNotifications(scope, state)
    override val importComponent = DefaultCoverLetterImportComponent(context) { imported ->
        val current = state.value.record
        state.value = state.value.copy(
            record = current.copy(
                name = imported.name,
                content = imported.content,
                keywords = imported.keywords,
            ),
            notice = LetterNotice.None,
        )
    }
    override val exportComponent = DefaultCoverLetterExportComponent(
        context,
        coverLetter = { state.value.record.takeIf { it.id > 0 } },
        finished = { onCloseExport() },
        exported = {
            if (it == CoverLetterExportFormat.Pdf) notices.show(LetterNotice.PdfSaved)
        },
    )
    init {
        val importing = importComponent.state.subscribe {
            state.value = state.value.copy(busy = it == CoverLetterImportState.Importing)
        }
        val exporting = exportComponent.state.subscribe {
            state.value = state.value.copy(busy = it == CoverLetterExportState.Exporting)
        }
        context.lifecycle.doOnDestroy {
            importing.cancel()
            exporting.cancel()
            scope.cancel()
        }
    }
    override fun onChange(record: CoverLetter) {
        val old = state.value
        if (!old.busy &&
            !old.confirming && !old.exportVisible
        ) {
            state.value =
                old.copy(
                    record = old.record.copy(
                        name = record.name,
                        content = record.content,
                        keywords = record.keywords,
                    ),
                )
        }
    }
    override fun onSave() = save(LetterSaveAction.Save)
    private fun save(action: LetterSaveAction) {
        if (state.value.busy || state.value.confirming || state.value.exportVisible) return
        val record = state.value.record
        val validation = when {
            record.name.isBlank() -> LetterNotice.Validation
            else -> LetterNotice.None
        }
        if (validation != LetterNotice.None) {
            notices.show(validation)
            return
        }
        perform("Save cover letter", LetterNotice.SaveFailed) {
            val saved = context.dependencies.coverLettersUseCases.save(record)
            state.value = state.value.copy(
                record = saved,
                exportVisible = action == LetterSaveAction.Export,
            )
            notices.show(LetterNotice.Saved)
        }
    }
    override fun onDelete() {
        if (state.value.busy || state.value.exportVisible || state.value.record.id == 0L) return
        if (!state.value.confirming) {
            state.value = state.value.copy(confirming = true)
            return
        }
        val id = state.value.record.id
        perform("Delete cover letter", LetterNotice.DeleteFailed) {
            context.dependencies.coverLettersUseCases.delete(id)
            deleted(id)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun perform(operation: String, failure: LetterNotice, action: suspend () -> Unit) {
        state.value = state.value.copy(busy = true, notice = LetterNotice.None)
        scope.launch {
            try {
                action()
            } catch (
                error: CancellationException,
            ) {
                throw error
            } catch (error: Exception) {
                context.dependencies.logger.error(
                    operation,
                    error,
                    mapOf("letterId" to state.value.record.id),
                )
                notices.show(failure)
            } finally {
                state.value = state.value.copy(busy = false)
            }
        }
    }
    override fun onCancelDelete() {
        if (!state.value.busy) {
            state.value =
                state.value.copy(confirming = false)
        }
    }
    override fun onImport() {
        if (!state.value.busy && !state.value.exportVisible && state.value.record.id == 0L) {
            importComponent.onImport()
        }
    }
    override fun onExport() {
        if (state.value.record.id > 0) save(LetterSaveAction.Export)
    }
    override fun onCloseExport() {
        if (exportComponent.state.value != CoverLetterExportState.Exporting) {
            state.value = state.value.copy(exportVisible = false)
            exportComponent.onReset()
        }
    }
    override fun onClose() {
        if (!state.value.busy) {
            when {
                state.value.exportVisible -> onCloseExport()

                state.value.confirming -> onCancelDelete()

                else -> {
                    importComponent.onClose()
                    close()
                }
            }
        }
    }
}

private enum class LetterSaveAction { Save, Export }
