package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.ui.components.common.ai.AiRequestDialogSlot

class TestVacancyResumeAssessmentComponent : VacancyResumeAssessmentComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())
    override val selection = MutableValue(ResumeAssessmentSelection())
    var requestedId: Long? = null
    override fun onAssess(id: Long) {
        requestedId = id
    }
    override fun onView(id: Long) {
        selection.value = ResumeAssessmentSelection(id)
    }
    override fun onCloseResult() {
        selection.value = ResumeAssessmentSelection()
    }
    override fun onCancel() {
        dialog.value.component?.onClose()
    }
}
