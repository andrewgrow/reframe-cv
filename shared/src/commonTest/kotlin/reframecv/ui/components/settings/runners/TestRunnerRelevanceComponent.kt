package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import reframecv.domain.models.providers.RunnerResult
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.components.common.ai.TestAiRequestDialogComponent

class TestRunnerRelevanceComponent(private val rows: () -> List<RunnerResult>) :
    RunnerRelevanceComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())
    override val selection = MutableValue(RunnerRelevanceSelection())
    var requested: Long? = null
    override fun assess(id: Long) {
        requested = id
        dialog.value = AiRequestDialogSlot(
            TestAiRequestDialogComponent(
                AiRequestDialogState("result #$id (runner-result.json)", loading = false),
            ) {
                dialog.value = AiRequestDialogSlot()
            },
        )
    }
    override fun view(id: Long) {
        selection.value = RunnerRelevanceSelection(rows().find { it.id == id })
    }
    override fun closeResult() {
        selection.value = RunnerRelevanceSelection()
    }
    override fun onBack(): Boolean {
        val handled = dialog.value.component != null || selection.value.result != null
        dialog.value.component?.onClose()
        closeResult()
        return handled
    }
}
