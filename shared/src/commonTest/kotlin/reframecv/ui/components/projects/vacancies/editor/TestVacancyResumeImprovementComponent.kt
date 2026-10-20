package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import reframecv.ui.components.common.ai.AiRequestDialogSlot

class TestVacancyResumeImprovementComponent : VacancyResumeImprovementComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())
    var requestedId: Long? = null
    override fun onImprove(id: Long) {
        requestedId = id
    }
    override fun onCancel() {
        dialog.value.component?.onClose()
    }
}
