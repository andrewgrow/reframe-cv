package ankideckbuilder.ui.components.projects.editor

import com.arkivanov.decompose.ComponentContext

interface EditorComponent {
    fun onClose()
}

class DefaultEditorComponent(
    componentContext: ComponentContext,
    private val onClosed: () -> Unit,
) : EditorComponent,
    ComponentContext by componentContext {
    override fun onClose() = onClosed()
}
