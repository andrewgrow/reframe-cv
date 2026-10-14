package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.resume.content.ResumeContact
import reframecv.domain.models.resume.content.ResumeContactsSection
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent

@OptIn(ExperimentalTestApi::class)
class ResumeContactFieldsTest : GoldenScreenshotTest() {
    @Test
    fun contactRemovalRequiresConfirmationAndPreservesOtherContacts() = runDesktopComposeUiTest {
        val first = ResumeContact("email", "Work email", "alex@example.com")
        val second = ResumeContact("phone", "", "+1234567")
        val draft = ResumeDraft("Mobile")
            .addSection(ResumeContactsSection(entries = listOf(first, second)))
        val component = TestResumeEditorComponent(draft)
        setGoldenContent(this) { ResumeEditorContent(component) }
        onAllNodesWithText("Remove Contact")[0].performScrollTo().performClick()
        assertEquals(draft, component.draft.value)
        onNodeWithTag("confirmation.input").assertDoesNotExist()
        onNode(
            hasText("Cancel") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        assertEquals(draft, component.draft.value)
        onAllNodesWithText("Remove Contact")[0].performScrollTo().performClick()
        onNodeWithTag("resume.item.remove.dialog").performKeyInput { pressKey(Key.Escape) }
        assertEquals(draft, component.draft.value)
        onAllNodesWithText("Remove Contact")[0].performScrollTo().performClick()
        onNode(
            hasText("Remove Contact") and hasAnyAncestor(hasTestTag("resume.item.remove.dialog")),
        ).performClick()
        val remaining = component.draft.value.sections.single().value as ResumeContactsSection
        assertEquals(listOf(second), remaining.entries)
    }

    @Test
    fun typePickerUsesStableValuesAndSharesEqualWidthRowWithOptionalLabel() =
        runDesktopComposeUiTest {
            val original = ResumeContact(value = "https://example.com")
            val component = TestResumeEditorComponent(
                ResumeDraft("Mobile").addSection(ResumeContactsSection(entries = listOf(original))),
            )
            setGoldenContent(this) { ResumeEditorContent(component) }
            val type = onNodeWithTag("resume.contact.type").performScrollTo()
            val label = onNodeWithTag("resume.contact.label")
            val typeBounds = type.fetchSemanticsNode().boundsInRoot
            val labelBounds = label.fetchSemanticsNode().boundsInRoot
            assertEquals(typeBounds.top, labelBounds.top)
            assertEquals(typeBounds.width, labelBounds.width, 1f)
            listOf("email", "phone", "linkedin", "website", "github", "other", "")
                .forEach { choice ->
                    type.performClick()
                    onNodeWithTag("resume.contact.type.option.$choice").performClick()
                    val section = component.draft.value.sections.single().value
                        as ResumeContactsSection
                    assertEquals(listOf(original.copy(type = choice)), section.entries)
                }
            label.performTextReplacement("My portfolio")
            type.performClick()
            onNodeWithTag("resume.contact.type.option.website").performClick()
            val stored = component.draft.value.toResume(1).content.sections.single()
                as ResumeContactsSection
            assertEquals(original.copy(type = "website", label = "My portfolio"), stored.entries[0])
            runOnIdle { component.saveState.value = ResumeSaveState.Saving }
            type.assertIsNotEnabled()
            label.assertIsNotEnabled()
            onNodeWithTag("resume.contact.value").assertIsNotEnabled()
        }

    @Test
    fun customStoredTypeRemainsSelectableAndEmptyLabelIsValid() = runDesktopComposeUiTest {
        val original = ResumeContact(type = "mastodon", value = "https://example.com/@alex")
        val draft = ResumeDraft("Mobile")
            .addSection(ResumeContactsSection(entries = listOf(original)))
        val component = TestResumeEditorComponent(draft)
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.contact.type").performScrollTo()
            .assertTextContains("mastodon").performClick()
        onNodeWithTag("resume.contact.type.option.mastodon").performClick()
        assertEquals(draft, component.draft.value)
        assertEquals(
            listOf(original),
            (component.draft.value.toResume(1).content.sections.single() as ResumeContactsSection)
                .entries,
        )
    }
}
