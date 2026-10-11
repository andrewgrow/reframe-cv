package reframecv.ui.components.projects.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.ui.context.AppComponentContext

/** Lets the project editor show save progress and failures. */
enum class EditorSaveState { Idle, Saving, Failed }

/** Lets the project editor show deletion progress and failures. */
enum class EditorDeleteState { Idle, Deleting, Failed }

internal const val DELETE_CONFIRMATION = "DELETE"

internal fun String.isDeleteConfirmation(): Boolean = equals(DELETE_CONFIRMATION, ignoreCase = true)

/** Lets one dialog create, rename, or delete a project. */
interface EditorComponent {
    val initialName: String?
    val saveState: Value<EditorSaveState>
    val deleteState: Value<EditorDeleteState>
    fun onSave(name: String)
    fun onClose()
    fun onDelete(confirmation: String)
}

/** Prevents blank project names, accidental deletion, and dismissal during unfinished changes. */
class DefaultEditorComponent(
    componentContext: AppComponentContext,
    private val onClosed: () -> Unit,
    private val onSaved: (String) -> Unit,
    override val initialName: String? = null,
    private val onDeleted: () -> Unit = {},
) : EditorComponent,
    AppComponentContext by componentContext {
    override val saveState = MutableValue(EditorSaveState.Idle)
    override val deleteState = MutableValue(EditorDeleteState.Idle)
    private val isBusy: Boolean
        get() = saveState.value == EditorSaveState.Saving ||
            deleteState.value == EditorDeleteState.Deleting

    override fun onSave(name: String) {
        if (name.isNotBlank() && !isBusy) onSaved(name.trim())
    }

    override fun onClose() {
        if (!isBusy) onClosed()
    }

    override fun onDelete(confirmation: String) {
        if (initialName != null && confirmation.isDeleteConfirmation() && !isBusy) onDeleted()
    }
}
