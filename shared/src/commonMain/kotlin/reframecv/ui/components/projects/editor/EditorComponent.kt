package reframecv.ui.components.projects.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.ui.context.AppComponentContext

enum class EditorSaveState { Idle, Saving, Failed }

interface EditorComponent {
    val saveState: Value<EditorSaveState>
    fun onSave(name: String)
    fun onClose()
}

class DefaultEditorComponent(
    componentContext: AppComponentContext,
    private val onClosed: () -> Unit,
    private val onSaved: (String) -> Unit,
) : EditorComponent,
    AppComponentContext by componentContext {
    override val saveState = MutableValue(EditorSaveState.Idle)

    override fun onSave(name: String) {
        if (name.isNotBlank() && saveState.value != EditorSaveState.Saving) onSaved(name.trim())
    }

    override fun onClose() {
        if (saveState.value != EditorSaveState.Saving) onClosed()
    }
}
