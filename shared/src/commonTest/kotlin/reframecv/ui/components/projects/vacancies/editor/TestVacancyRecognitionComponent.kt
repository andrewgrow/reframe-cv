package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestDialogState
import reframecv.ui.components.common.ai.TestAiRequestDialogComponent

class TestVacancyRecognitionComponent : VacancyRecognitionComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())
    override val state = MutableValue<VacancyRecognitionState>(VacancyRecognitionState.Idle)
    override fun onRecognize() {
        dialog.value = AiRequestDialogSlot(
            TestAiRequestDialogComponent(
                AiRequestDialogState("job.pdf", loading = false),
                close = ::onCancel,
            ),
        )
    }
    override fun onCancel() {
        dialog.value = AiRequestDialogSlot()
        state.value = VacancyRecognitionState.Idle
    }
    override fun onReset() {
        if (state.value !=
            VacancyRecognitionState.Processing
        ) {
            state.value = VacancyRecognitionState.Idle
        }
    }
}
