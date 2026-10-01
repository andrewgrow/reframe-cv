package ankideckbuilder.ui.components.projects.editor

class TestEditorComponent(private val onCloseClick: () -> Unit = {}) : EditorComponent {
    override fun onClose() = onCloseClick()
}
