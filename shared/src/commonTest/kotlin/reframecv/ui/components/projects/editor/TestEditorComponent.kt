package reframecv.ui.components.projects.editor

import com.arkivanov.decompose.value.MutableValue

class TestEditorComponent(
    override val initialName: String? = null,
    private val onCloseClick: () -> Unit = {},
) : EditorComponent {
    override val saveState = MutableValue(EditorSaveState.Idle)
    override fun onSave(name: String) = onCloseClick()
    override fun onClose() = onCloseClick()
}
