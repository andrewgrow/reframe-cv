package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacancyEditorContentTest {
    @Test
    fun requiresNameAndSubmitsBothNamesWithEnter() = runComposeUiTest {
        val drafts = mutableListOf<VacancyDraft>()
        val component = TestVacancyEditorComponent(save = { drafts += it })
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.name").assertIsFocused()
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithTag("vacancy.name").performTextReplacement("   ")
        onNodeWithTag("vacancy.name").performImeAction()
        assertEquals(emptyList(), drafts)
        onNodeWithTag("vacancy.jobTitle").performTextReplacement("Senior Android Developer")
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithTag("vacancy.name").performTextReplacement("Android Developer")
        onNodeWithTag("vacancy.company").performTextReplacement("Example")
        onNodeWithTag("vacancy.description").performTextReplacement("Job description\nRequirements")
        onNodeWithTag("vacancy.url").performScrollTo().performTextReplacement("https://example.com")
        onNodeWithTag(
            "vacancy.keywords",
        ).performScrollTo().performTextReplacement("Kotlin, Android")
        onNodeWithTag("vacancy.keywords").performImeAction()
        assertEquals(
            listOf(
                VacancyDraft(
                    "Android Developer",
                    "Example",
                    "Job description\nRequirements",
                    "https://example.com",
                    "Kotlin, Android",
                    jobTitle = "Senior Android Developer",
                ),
            ),
            drafts,
        )
    }

    @Test
    fun keepsDraftAfterFailureAndBlocksActionsWhileSaving() = runComposeUiTest {
        var closes = 0
        var saves = 0
        val component = TestVacancyEditorComponent(save = { saves++ }, close = { closes++ })
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithTag("vacancy.name").performTextReplacement("Backend")
        onNodeWithText("Save").performClick()
        assertEquals(1, saves)
        runOnIdle { component.saveState.value = VacancySaveState.Failed }
        onNodeWithText("Could not save the vacancy. Please try again.").performScrollTo()
        onNodeWithTag("vacancy.name").performScrollTo()
        onNodeWithText("Backend").assertExists()
        onNodeWithText("Save").assertIsEnabled()
        runOnIdle { component.saveState.value = VacancySaveState.Saving }
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("vacancy.name").assertIsNotEnabled()
        runOnIdle { component.saveState.value = VacancySaveState.Idle }
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closes)
    }

    @Test
    fun editingPrefillsFieldsAndCancelDoesNotSubmitChanges() = runComposeUiTest {
        val original = Vacancy(
            id = 7, projectId = 1, name = "Backend", jobTitle = "Senior Backend Developer",
            company = "Example",
            description = "Job description", url = "https://example.com",
            keywords = listOf(
                "Kotlin",
                "Java",
            ),
            createdAt = 100, updatedAt = 200,
        )
        var closes = 0
        val saved = mutableListOf<VacancyDraft>()
        val component = TestVacancyEditorComponent(
            initialVacancy = original,
            save = { saved += it },
            close = { closes++ },
        )
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Edit vacancy").assertExists()
        onNodeWithText("Backend").assertExists()
        onNodeWithText("Senior Backend Developer").assertExists()
        onNodeWithText("Example").assertExists()
        onNodeWithText("Job description").assertExists()
        onNodeWithText("https://example.com").assertExists()
        onNodeWithText("Kotlin, Java").assertExists()
        onNodeWithTag("vacancy.name").assertIsFocused().performTextReplacement("Changed")
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closes)
        assertEquals(emptyList(), saved)
    }

    @Test
    fun listFieldsPreserveTypingResizeIndependentlyAndSaveTrimmedItems() =
        runDesktopComposeUiTest(width = 800, height = 600) {
            val original = Vacancy(
                id = 7,
                projectId = 1,
                name = "Backend",
                createdAt = 0,
                updatedAt = 0,
                responsibilities = listOf("Build, test, ship", "Review code"),
                requirements = listOf("Kotlin"),
                niceToHave = listOf("German"),
            )
            val saved = mutableListOf<VacancyDraft>()
            val component = TestVacancyEditorComponent(
                initialVacancy = original,
                save = { saved += it },
            )
            setContent { ReframeTheme { VacancyEditorContent(component) } }
            val tags = listOf("responsibilities", "requirements", "niceToHave")
            val values = listOf("Build, test, ship\nReview code", "Kotlin", "German")
            tags.zip(values).forEach { (tag, value) ->
                val field = onNodeWithTag("vacancy.$tag").performScrollTo()
                onNodeWithText(value).assertExists()
                val initialHeight = field.fetchSemanticsNode().size.height
                onNodeWithTag("vacancy.$tag.resize").performScrollTo().performMouseInput {
                    moveTo(center)
                    press()
                    moveBy(Offset(0f, 15f))
                    moveBy(Offset(0f, 45f))
                    release()
                }
                val expanded = field.fetchSemanticsNode().size.height
                assertTrue(expanded > initialHeight + 20)
                field.performScrollTo().performTextReplacement("  $value  \n\n  Extra, item  \n")
                assertEquals(expanded, field.fetchSemanticsNode().size.height)
                onNodeWithText("  $value  \n\n  Extra, item  \n").assertExists()
                onNodeWithTag("vacancy.$tag.resize").performScrollTo().performMouseInput {
                    moveTo(center)
                    press()
                    moveBy(Offset(0f, -15f))
                    moveBy(Offset(0f, -60f))
                    release()
                }
                assertTrue(field.fetchSemanticsNode().size.height < expanded)
            }
            onNodeWithText("Save").performClick()
            val vacancy = saved.single().applyTo(original)
            assertEquals(
                listOf("Build, test, ship", "Review code", "Extra, item"),
                vacancy.responsibilities,
            )
            assertEquals(listOf("Kotlin", "Extra, item"), vacancy.requirements)
            assertEquals(listOf("German", "Extra, item"), vacancy.niceToHave)
            runOnIdle { component.saveState.value = VacancySaveState.Saving }
            tags.forEach {
                onNodeWithTag("vacancy.$it").assertIsNotEnabled()
                onNodeWithTag("vacancy.$it.resize").assertIsNotEnabled()
            }
        }

    @Test
    fun escapeClosesVacancyAfterHoveringActionsWithoutClicks() = runDesktopComposeUiTest {
        var closes = 0
        var writes = 0
        var deletes = 0
        val vacancy = Vacancy(id = 1, projectId = 1, name = "Mobile", createdAt = 0, updatedAt = 0)
        val component = TestVacancyEditorComponent(
            initialVacancy = vacancy,
            save = { writes++ },
            close = { closes++ },
            delete = { deletes++ },
        )
        setContent { ReframeTheme { VacancyEditorContent(component) } }
        onNodeWithText("Save").performMouseInput { enter(center) }
        onNodeWithText("Delete").performMouseInput { moveTo(center) }
        onNodeWithText("Cancel").performMouseInput { moveTo(center) }
        onNodeWithTag("vacancies.editor").performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, closes)
        assertEquals(0, writes)
        assertEquals(0, deletes)
    }
}
