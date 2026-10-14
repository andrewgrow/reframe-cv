package reframecv.ui.components.projects.resumes

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.Resume
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.DefaultResumeEditorComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.threading.runOnUiThread

class ResumeValidationComponentTest : ComponentTest() {
    @Test
    fun onlyInvalidSaveEmitsValidationEventsAndCorrectionsDoNotResetThem() = runOnUiThread {
        lifecycle.resume()
        var saves = 0
        val component = DefaultResumeEditorComponent(
            appComponentContext(),
            save = { saves++ },
            close = {},
            initialResume = Resume(
                id = 1,
                projectId = 1,
                name = "Mobile",
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        component.onDraftChange(ResumeDraft())
        component.onExport()
        assertEquals(0L, component.validationFailures.value)
        component.onSave()
        component.onSave()
        assertEquals(2L, component.validationFailures.value)
        assertEquals(0, saves)
        component.onDraftChange(ResumeDraft("Mobile"))
        assertEquals(2L, component.validationFailures.value)
        component.onSave()
        assertEquals(1, saves)
        assertEquals(2L, component.validationFailures.value)
    }
}
