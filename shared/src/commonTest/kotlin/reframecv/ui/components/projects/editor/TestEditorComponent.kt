package reframecv.ui.components.projects.editor

import com.arkivanov.decompose.value.MutableValue

class TestEditorComponent(
    override val initialName: String? = null,
    private val onDeleteClick: (String) -> Unit = {},
    private val onCloseClick: () -> Unit = {},
) : EditorComponent {
    override val saveState = MutableValue(EditorSaveState.Idle)
    override val deleteState = MutableValue(EditorDeleteState.Idle)
    override fun onDelete(confirmation: String) = onDeleteClick(confirmation)
    override fun onSave(name: String) = onCloseClick()
    override fun onClose() = onCloseClick()
}
