package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.recognition.RecognitionError

class TestVacancyRecognitionComponent : VacancyRecognitionComponent {
    override val state = MutableValue<VacancyRecognitionState>(VacancyRecognitionState.Idle)
    override fun onRecognize() {
        state.value =
            VacancyRecognitionState.Failed(RecognitionError.NotConfigured)
    }
    override fun onCancel() {
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
