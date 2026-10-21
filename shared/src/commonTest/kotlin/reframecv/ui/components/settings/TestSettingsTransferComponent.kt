package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue

class TestSettingsTransferComponent(initial: SettingsTransferState = SettingsTransferState()) :
    SettingsTransferComponent {
    override val state = MutableValue(initial)
    var exports = 0
    var imports = 0
    override fun onIncludeApiKeys(value: Boolean) {
        state.value = state.value.copy(includeApiKeys = value)
    }
    override fun onImport() {
        imports++
    }
    override fun onExport() {
        exports++
    }
    override fun onBack() = Unit
}
