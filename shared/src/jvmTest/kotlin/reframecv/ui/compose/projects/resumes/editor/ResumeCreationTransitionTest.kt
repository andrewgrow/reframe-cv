package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.dependencies.TestApplicationDependencies
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.DefaultResumesComponent
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.compose.projects.resumes.ResumesContent
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

@OptIn(ExperimentalTestApi::class)
class ResumeCreationTransitionTest : GoldenScreenshotTest() {
    @Test
    fun successfulCreationChangesActionsAndFurtherSaveUpdatesTheSameResume() =
        runDesktopComposeUiTest {
            val services = TestApplicationDependencies()
            val lifecycle = LifecycleRegistry()
            val component = runOnUiThread {
                lifecycle.resume()
                DefaultResumesComponent(
                    DefaultAppComponentContext(DefaultComponentContext(lifecycle), services),
                    7,
                    back = {
                    },
                ).also { it.onAdd() }
            }
            try {
                setGoldenContent(this) { ResumesContent(component) }
                onNodeWithTag("resume.name").performTextReplacement("First")
                onNodeWithText("Save").performClick()
                waitUntil { component.editor?.saveState?.value == ResumeSaveState.Saved }
                val id = services.resumesRepository.records.value.single().id
                onNodeWithText("Resume").assertExists()
                onNodeWithText("Import").assertDoesNotExist()
                onNodeWithText("Cancel").assertDoesNotExist()
                onNodeWithText("Close").assertExists()
                onNodeWithText("Delete").assertExists()
                onNodeWithText("Export").performClick()
                onNodeWithTag("resume.export.dialog").assertExists()
                onNodeWithText("Download").performClick()
                waitUntil { component.editor?.exportVisible?.value == false }
                onNodeWithTag("resume.export.dialog").assertDoesNotExist()
                onNodeWithTag("resume.name").performTextReplacement("Updated")
                onNodeWithText("Save").performClick()
                waitUntil { component.editor?.saveState?.value == ResumeSaveState.Saved }
                assertEquals(id, services.resumesRepository.records.value.single().id)
                assertEquals("Updated", services.resumesRepository.records.value.single().name)
                onNodeWithText("Close").performClick()
                onNodeWithTag("resumes.editor").assertDoesNotExist()
            } finally {
                runOnUiThread { lifecycle.destroy() }
            }
        }
}
