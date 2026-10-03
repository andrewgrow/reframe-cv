package reframecv.ui.components.projects.editor

import com.arkivanov.decompose.value.MutableValue

class TestEditorComponent(private val onCloseClick: () -> Unit = {}) : EditorComponent {
    override val saveState = MutableValue(EditorSaveState.Idle)
    override fun onSave(name: String) = onCloseClick()
    override fun onClose() = onCloseClick()
}
