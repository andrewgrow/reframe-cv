package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.resume.content.ResumeLanguage
import reframecv.domain.models.resume.content.ResumeLanguagesSection
import reframecv.domain.models.resume.content.ResumeSkillGroup
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeSectionEditorTest : GoldenScreenshotTest() {
    @Test
    fun languageRemovalStaysCloseToFieldsAndRequiresConfirmation() = runDesktopComposeUiTest {
        val keep = ResumeLanguage("German", "B2")
        val discard = ResumeLanguage("English", "C1")
        val draft = ResumeDraft("Mobile").addSection(
            ResumeLanguagesSection(entries = listOf(discard, keep)),
        )
        var saves = 0
        val component = TestResumeEditorComponent(draft, save = { saves++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        val remove = onAllNodesWithText("Remove Language")[0].performScrollTo()
        val fields = onNode(hasText("C1"))
        val buttonBounds = remove.fetchSemanticsNode().boundsInRoot
        val fieldBounds = fields.fetchSemanticsNode().boundsInRoot
        assertTrue(buttonBounds.center.x > fieldBounds.center.x)
        assertTrue(buttonBounds.top - fieldBounds.bottom in 0f..8f)
        remove.performClick()
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        assertEquals(draft, component.draft.value)
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        assertEquals(draft, component.draft.value)
        remove.performScrollTo().performClick()
        onNodeWithTag("resume.item.remove.dialog").performKeyInput { pressKey(Key.Escape) }
        assertEquals(draft, component.draft.value)
        remove.performScrollTo().performClick()
        onNode(
            hasText("Remove Language") and
                hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        val remaining = component.draft.value.sections.single().value as ResumeLanguagesSection
        assertEquals(listOf(keep), remaining.entries)
        assertEquals(0, saves)
    }

    @Test
    fun skillGroupRemovalStaysCloseToFieldsAndRequiresConfirmation() = runDesktopComposeUiTest {
        val keep = ResumeSkillGroup("Keep", listOf("Compose"))
        val discard = ResumeSkillGroup("Discard", listOf("Kotlin"))
        val draft = ResumeDraft("Mobile").addSection(
            ResumeSkillsSection(groups = listOf(discard, keep)),
        )
        var saves = 0
        val component = TestResumeEditorComponent(draft, save = { saves++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        val remove = onAllNodesWithText("Remove Skill Group")[0].performScrollTo()
        val fields = onNode(hasText("Kotlin"))
        val buttonBounds = remove.fetchSemanticsNode().boundsInRoot
        val fieldBounds = fields.fetchSemanticsNode().boundsInRoot
        assertTrue(buttonBounds.center.x > fieldBounds.center.x)
        assertTrue(buttonBounds.top - fieldBounds.bottom in 0f..8f)
        remove.performClick()
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        assertEquals(draft, component.draft.value)
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        assertEquals(draft, component.draft.value)
        remove.performScrollTo().performClick()
        onNodeWithTag("resume.item.remove.dialog").performKeyInput { pressKey(Key.Escape) }
        assertEquals(draft, component.draft.value)
        remove.performScrollTo().performClick()
        onNode(
            hasText("Remove Skill Group") and
                hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        val remaining = component.draft.value.sections.single().value as ResumeSkillsSection
        assertEquals(listOf(keep), remaining.groups)
        assertEquals(0, saves)
    }

    @Test
    fun sectionRemovalRequiresSecondClickAndCancelOrEscapeKeepsTheDraft() =
        runDesktopComposeUiTest {
            var saves = 0
            var closes = 0
            val draft = ResumeDraft("Mobile")
                .addSection(ResumeSummarySection(text = "Keep"))
                .addSection(ResumeSummarySection(text = "Discard this summary"))
            val component =
                TestResumeEditorComponent(draft, save = { saves++ }, close = { closes++ })
            setGoldenContent(this) { ResumeEditorContent(component) }
            val remove = onNode(
                hasText("Remove Summary Section") and
                    hasAnyAncestor(hasTestTag("resume.section.2")),
            )
            remove.performScrollTo().performClick()
            onNodeWithTag("resume.section.remove.dialog").assertExists()
            onNodeWithTag("confirmation.input").assertDoesNotExist()
            assertEquals(2, component.draft.value.sections.size)
            onNode(
                hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.section.remove.dialog")),
            ).performClick()
            onNodeWithTag("resume.section.remove.dialog").assertDoesNotExist()
            assertEquals(draft, component.draft.value)
            remove.performScrollTo().performClick()
            onNodeWithTag("resume.section.remove.dialog").performKeyInput { pressKey(Key.Escape) }
            onNodeWithTag("resume.section.remove.dialog").assertDoesNotExist()
            assertEquals(draft, component.draft.value)
            remove.performScrollTo().performClick()
            onNode(
                hasText("Remove Summary Section") and
                    hasAnyAncestor(hasTestTag("resume.section.remove.dialog")),
            ).performClick()
            onNodeWithTag("resume.section.remove.dialog").assertDoesNotExist()
            assertEquals(listOf(draft.sections.first()), component.draft.value.sections)
            assertEquals(0, saves)
            assertEquals(0, closes)
        }

    @Test
    fun visibilitySharesTheControlRowAndRemoveStaysAtTheRightEdge() =
        runDesktopComposeUiTest(ScreenSize(1000.dp, 600.dp)) {
            val component = TestResumeEditorComponent(
                ResumeDraft("Mobile").addSection(ResumeSummarySection(text = "Summary")),
            )
            setGoldenContent(this, screenSize = ScreenSize(1000.dp, 600.dp)) {
                ResumeEditorContent(component)
            }
            val section = hasAnyAncestor(hasTestTag("resume.section.1"))
            val collapse = onNode(hasText("Collapse") and section).performScrollTo()
            val include = onNode(hasText("Include in resume") and section)
            val remove = onNode(hasText("Remove Summary Section") and section)
            assertEquals(
                collapse.fetchSemanticsNode().boundsInRoot.top,
                include.fetchSemanticsNode().boundsInRoot.top,
            )
            assertTrue(
                remove.fetchSemanticsNode().boundsInRoot.right >
                    include.fetchSemanticsNode().boundsInRoot.right,
            )
            include.performClick()
            assertEquals(false, component.draft.value.sections.single().value.visible)
        }
}
