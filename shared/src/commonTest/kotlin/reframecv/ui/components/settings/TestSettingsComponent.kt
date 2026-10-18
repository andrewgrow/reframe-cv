package reframecv.ui.components.settings

import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.providers.ProviderConnection
import reframecv.ui.components.settings.prompts.PromptsComponent

class TestSettingsComponent(
    initial: SettingsState = SettingsState(loading = false),
    editor: ProviderEditorState? = null,
    prompts: PromptsComponent? = null,
) : SettingsComponent {
    override val state = MutableValue(initial)
    override val editorSlot = MutableValue<ChildSlot<*, ProviderEditorComponent>>(
        editor?.let { ChildSlot(child = Child.Created(Unit, TestProviderEditorComponent(it))) }
            ?: ChildSlot<Unit, ProviderEditorComponent>(),
    )
    override val promptsSlot =
        MutableValue<ChildSlot<*, PromptsComponent>>(
            prompts?.let {
                ChildSlot(child = Child.Created(Unit, it))
            }
                ?: ChildSlot<Unit, PromptsComponent>(),
        )
    override fun onProviders() = Unit
    override fun onPrompts() = Unit
    override fun onAdd() = Unit
    override fun onEdit(connection: ProviderConnection) = Unit
    override fun onActiveChange(id: Long, active: Boolean) = Unit
    override fun onRetry() = Unit
    override fun onBack() = Unit
}

class TestProviderEditorComponent(initial: ProviderEditorState) : ProviderEditorComponent {
    override val state = MutableValue(initial)
    override fun onName(value: String) {
        state.value =
            state.value.copy(connection = state.value.connection.copy(name = value))
    }
    override fun onKey(value: String) {
        state.value =
            state.value.copy(connection = state.value.connection.copy(apiKey = value))
    }
    override fun onManagementKey(value: String) {
        state.value =
            state.value.copy(connection = state.value.connection.copy(managementApiKey = value))
    }
    override fun onModel(id: String) {
        state.value =
            state.value.copy(connection = state.value.connection.copy(modelId = id))
    }
    override fun onActiveChange(active: Boolean) {
        state.value =
            state.value.copy(connection = state.value.connection.copy(active = active))
    }
    override fun onLoadModels() = Unit
    override fun onRefreshAccount() = Unit
    override fun onRefreshBalance() = Unit
    override fun onSave() = Unit
    override fun onBack() = Unit
}
