package reframecv.ui.components.projects.resumes

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.threading.runOnUiThread

class MainResumeComponentTest : ComponentTest() {
    @Test
    fun selectsNewMainRestoresCheckboxAndClearsOnlyOnUpdate() = runBlocking {
        lateinit var component: DefaultResumesComponent
        val selected = CompletableDeferred<ResumesState.Ready>()
        runOnUiThread {
            lifecycle.resume()
            component = DefaultResumesComponent(appComponentContext(), 7, back = {})
            component.uiState.subscribe { state ->
                if (state is ResumesState.Ready &&
                    state.mainResumeId != null
                ) {
                    selected.complete(state)
                }
            }
            component.onAdd()
            val editor = checkNotNull(component.editor)
            assertEquals(ResumeRole.Regular, editor.draft.value.role)
            editor.onDraftChange(ResumeDraft("Main", role = ResumeRole.Main))
            editor.onSave()
        }
        val ready = withTimeout(10_000.milliseconds) { selected.await() }
        val original = ready.resumes.single()
        assertEquals(original.id, ready.mainResumeId)
        val cleared = CompletableDeferred<Unit>()
        runOnUiThread {
            component.onEdit(original)
            val editor = checkNotNull(component.editor)
            assertEquals(ResumeRole.Main, editor.draft.value.role)
            editor.onDraftChange(editor.draft.value.copy(role = ResumeRole.Regular))
            component.onBack()
            assertEquals(original.id, dependencies.resumesRepository.mainResumeIds.value[7])
            component.onEdit(original)
            val reopened = checkNotNull(component.editor)
            assertEquals(ResumeRole.Main, reopened.draft.value.role)
            component.uiState.subscribe { state ->
                if (state is ResumesState.Ready &&
                    state.mainResumeId == null
                ) {
                    cleared.complete(Unit)
                }
            }
            reopened.onDraftChange(reopened.draft.value.copy(role = ResumeRole.Regular))
            reopened.onSave()
        }
        withTimeout(10_000.milliseconds) { cleared.await() }
        runOnUiThread {
            assertNull(dependencies.resumesRepository.mainResumeIds.value[7])
            assertEquals(1, assertIs<ResumesState.Ready>(component.uiState.value).resumes.size)
        }
    }
}
