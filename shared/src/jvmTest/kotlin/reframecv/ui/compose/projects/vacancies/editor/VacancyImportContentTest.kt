package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.dependencies.TestApplicationDependencies
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyImportState
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyImportContentTest {
    @Test
    fun creationAllowsImportWithoutTitleAndManagementHidesIt() = runComposeUiTest {
        var imports = 0
        val component = TestVacancyEditorComponent(imported = { imports++ })
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Import").assertIsEnabled().performClick()
        runOnIdle { assertEquals(1, imports) }
        onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun busyImportShowsProgressAndPreventsEscapeAndOtherActions() = runComposeUiTest {
        var closes = 0
        val component = TestVacancyEditorComponent(close = { closes++ })
        component.importComponent.state.value = VacancyImportState.Importing
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.import.progress").assertExists()
        onNodeWithText("Importing vacancy...").assertExists()
        onNodeWithText("Import").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithText("Attach source file").assertIsNotEnabled()
        onRoot().performKeyInput {
            keyDown(Key.Escape)
            keyUp(Key.Escape)
        }
        runOnIdle { assertEquals(0, closes) }
        runOnIdle { component.importComponent.state.value = VacancyImportState.Failed }
        onNodeWithText(
            "Could not import the vacancy. Choose a valid Reframe CV vacancy JSON file and try again.",
        ).assertExists()
        onNodeWithText("Import").assertIsEnabled()
    }

    @Test
    fun firstSaveReplacesImportWithManagementActionsAndUpdatesTheHeading() = runComposeUiTest {
        val lifecycle = LifecycleRegistry()
        lateinit var component: DefaultVacancyEditorComponent
        runOnUiThread {
            lifecycle.resume()
            component = DefaultVacancyEditorComponent(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    TestApplicationDependencies(),
                ),
                save = { draft ->
                    component.onSaveCompleted(
                        VacancySaveState.Idle,
                        draft.toVacancy(7).copy(id = 9),
                    )
                },
                close = {},
            )
        }
        try {
            setContent { ReframeTheme { VacancyEditorContent(component) } }
            onNodeWithText("Import").assertIsEnabled()
            onNodeWithTag("vacancy.title").performTextReplacement("New vacancy")
            onNodeWithText("Save").performClick()
            onNodeWithText("Edit vacancy").assertExists()
            onNodeWithText("Import").assertDoesNotExist()
            onNodeWithText("Export").assertIsEnabled()
            onNodeWithText("Delete").assertIsEnabled()
        } finally {
            runOnUiThread { lifecycle.destroy() }
        }
    }

    @Test
    fun managementOnlyOffersExport() = runComposeUiTest {
        val original =
            Vacancy(id = 1, projectId = 7, name = "Backend", createdAt = 100, updatedAt = 100)
        setContent { ReframeTheme { VacancyEditorContent(TestVacancyEditorComponent(original)) } }
        onNodeWithText("Import").assertDoesNotExist()
        onNodeWithText("Export").assertIsEnabled()
    }
}
