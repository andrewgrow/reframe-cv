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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeEducationSection
import reframecv.domain.models.resume.content.ResumeExperienceSection
import reframecv.domain.models.resume.content.ResumeSkillGroup
import reframecv.domain.models.resume.content.ResumeSkillsSection
import reframecv.domain.models.resume.content.ResumeSoftSkillsSection
import reframecv.domain.models.resume.content.ResumeSummarySection
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.compose.application.RootContent

@OptIn(ExperimentalTestApi::class)
class ResumeEditorContentTest : GoldenScreenshotTest() {
    @Test
    fun mainResumeCheckboxIsEditableAndBlockedDuringSaving() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(ResumeDraft("Main", role = ResumeRole.Main))
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Main resume").performClick()
        assertEquals(ResumeRole.Regular, component.draft.value.role)
        onNodeWithText("Main resume").performClick()
        assertEquals(ResumeRole.Main, component.draft.value.role)
        runOnIdle { component.saveState.value = ResumeSaveState.Saving }
        onNodeWithText("Main resume").assertIsNotEnabled()
    }

    @Test
    fun editingLoadsSectionsAndSubmitsChangesWithOriginalIdentity() = runDesktopComposeUiTest {
        val original = Resume(
            id = 12,
            projectId = 7,
            name = "Mobile",
            createdAt = 10,
            updatedAt = 20,
            content = sampleResumeContent(),
            sourceResumeId = 11,
        )
        var saved: Resume? = null
        val component = TestResumeEditorComponent(
            initialResume = original,
            save = { saved = it.applyTo(original) },
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Resume").assertExists()
        onNodeWithTag("resume.name").performTextReplacement("Backend")
        onNodeWithText(
            "Professional headline",
        ).performScrollTo().performTextReplacement("Backend engineer")
        val customSection = hasAnyAncestor(hasTestTag("resume.section.8"))
        onNode(hasText("Section title") and customSection).performScrollTo()
            .performTextReplacement("Certificates")
        onNode(hasText("Include in resume") and customSection).performScrollTo().performClick()
        onNode(hasText("Move up") and customSection).performScrollTo().performClick()
        onNodeWithText("Save").performClick()
        val result = checkNotNull(saved)
        assertEquals("Backend", result.name)
        assertEquals("Backend engineer", result.content.profile.headline)
        assertEquals(original.id, result.id)
        assertEquals(original.createdAt, result.createdAt)
        assertEquals(original.sourceResumeId, result.sourceResumeId)
        assertEquals("Certificates", result.content.sections[6].title)
        assertFalse(result.content.sections[6].visible)
        assertEquals(original.content, sampleResumeContent())
    }

    @Test
    fun skillListsUseCommasAndSaveTrimmedNonEmptyItems() = runDesktopComposeUiTest {
        var writes = 0
        val component = TestResumeEditorComponent(save = { writes++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        val sections = listOf(
            ResumeSkillsSection(
                groups = listOf(ResumeSkillGroup("Mobile", listOf(" Kotlin ", " Compose "))),
            ),
            ResumeSoftSkillsSection(items = listOf(" Kotlin ", " Compose ")),
        )
        sections.forEach { section ->
            runOnIdle { component.onDraftChange(ResumeDraft("Draft").addSection(section)) }
            onNodeWithText("Items (comma-separated)").performScrollTo()
                .assertTextContains("Kotlin, Compose")
                .performTextReplacement(" Kotlin, Compose, , Coroutines, ")
            // Typing preserves separators and spaces until Save converts the draft.
            val expected = listOf("Kotlin", "Compose", "Coroutines")
            onNodeWithText("Save").performClick()
            val saved = component.draft.value.toResume(1).content.sections.single()
            val items = when (saved) {
                is ResumeSkillsSection -> saved.groups.single().items
                is ResumeSoftSkillsSection -> saved.items
                else -> error("Unexpected section")
            }
            assertEquals(expected, items)
            assertEquals(
                " Kotlin, Compose, , Coroutines, ",
                when (val raw = component.draft.value.sections.single().value) {
                    is ResumeSkillsSection -> raw.groups.single().items.joinToString(",")
                    is ResumeSoftSkillsSection -> raw.items.joinToString(",")
                    else -> error("Unexpected section")
                },
            )
        }
        assertEquals(2, writes)
    }

    @Test
    fun validatesNameAndSavesProfileSummaryAndKeywordsWithFixedControls() =
        runDesktopComposeUiTest {
            var writes = 0
            val component = TestResumeEditorComponent(save = { writes++ })
            setGoldenContent(this) { ResumeEditorContent(component) }
            onNodeWithText("Save").performClick()
            onNodeWithText("Enter a resume name.").assertExists()
            assertEquals(0, writes)
            onNodeWithTag("resume.name").performTextReplacement("Mobile")
            onNodeWithText("Full name").performScrollTo().performTextReplacement("Alex")
            onNodeWithText(
                "Keywords (comma-separated)",
            ).performScrollTo().performTextReplacement("Kotlin, Compose")
            onNodeWithText("Add section").performScrollTo().performClick()
            onNodeWithText("Summary").performClick()
            onNodeWithText(
                "Text",
            ).performScrollTo().performTextReplacement("Builds reliable applications.")
            onNodeWithText("Save").performClick()
            assertEquals(1, writes)
            val resume = component.draft.value.toResume(7)
            assertEquals("Alex", resume.content.profile.name)
            assertEquals(listOf("Kotlin", "Compose"), resume.keywords)
            assertEquals(
                "Builds reliable applications.",
                assertIs<ResumeSummarySection>(resume.content.sections.single()).text,
            )
        }

    @Test
    fun educationDatesRejectPartialInvalidInputAndPresentOmitsEndDate() = runDesktopComposeUiTest {
        var writes = 0
        val component = TestResumeEditorComponent(save = { writes++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.name").performTextReplacement("Draft")
        onNodeWithText("Add section").performScrollTo().performClick()
        onNodeWithText("Education").performClick()
        onNodeWithText("Add entry").performScrollTo().performClick()
        onNodeWithText("Institution").performScrollTo().performTextReplacement("University")
        onNodeWithText("Start (YYYY or YYYY-MM)").performScrollTo().performTextReplacement("2020-")
        onNodeWithText("Save").performClick()
        assertEquals(0, writes)
        onNodeWithText("Use YYYY or YYYY-MM, with a valid year and month.").assertExists()
        onNodeWithText("Start (YYYY or YYYY-MM)").performScrollTo().performTextReplacement("2020")
        onNodeWithText("End (YYYY or YYYY-MM)").performScrollTo().performTextReplacement("invalid")
        onNodeWithText("Present").performScrollTo().performClick()
        onNodeWithText("Save").performClick()
        assertEquals(1, writes)
        val entry = assertIs<ResumeEducationSection>(
            component.draft.value.toResume(1).content.sections.single(),
        ).entries.single()
        assertEquals(2020, entry.period.start?.year)
        assertEquals(null, entry.period.end)
        assertEquals(true, entry.period.isCurrent)
    }

    @Test
    fun experienceSupportsProfessionalProjectsAndCategorizedTools() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent()
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.name").performTextReplacement("Developer")
        onNodeWithText("Add section").performScrollTo().performClick()
        onNodeWithText("Work experience").performClick()
        onNodeWithText("Add entry").performScrollTo().performClick()
        onNodeWithText("Position").performScrollTo().performTextReplacement("Mobile developer")
        onNodeWithText("Add project").performScrollTo().performClick()
        onNodeWithText("Project name").performScrollTo().performTextReplacement("Banking app")
        onNodeWithText("Add skill or tool").performScrollTo().performClick()
        onNodeWithText("Skill or tool").performScrollTo().performTextReplacement("Kotlin")
        onNodeWithText("Category").performScrollTo().performTextReplacement("Language")
        val entry = assertIs<ResumeExperienceSection>(
            component.draft.value.toResume(1).content.sections.single(),
        ).entries.single()
        assertEquals("Mobile developer", entry.position)
        assertEquals("Banking app", entry.projects.single().name)
        assertEquals("Language", entry.projects.single().skillsOrTools.single().category)
    }

    @Test
    fun cancelEscapeAndBusyStateBehaveLikeVacancyEditor() = runDesktopComposeUiTest {
        var closes = 0
        val component = TestResumeEditorComponent(close = { closes++ })
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithTag("resume.name").performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, closes)
        onNodeWithText("Cancel").performClick()
        assertEquals(2, closes)
        runOnIdle { component.saveState.value = ResumeSaveState.Saving }
        onNodeWithText("Save").assertIsNotEnabled()
        onNodeWithText("Cancel").assertIsNotEnabled()
        onNodeWithTag("resume.name").performKeyInput { pressKey(Key.Escape) }
        assertEquals(2, closes)
        assertFalse(component.draft.value.isValid)
    }

    @Test
    fun savedResumeShowsSuccessAndCloseDiscardsFurtherChanges() = runDesktopComposeUiTest {
        var writes = 0
        var closes = 0
        val resume = Resume(id = 12, projectId = 1, name = "Mobile", createdAt = 0, updatedAt = 0)
        val component = TestResumeEditorComponent(
            initialResume = resume,
            save = { writes++ },
            close = { closes++ },
        )
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Cancel").assertDoesNotExist()
        onNodeWithText("Save").performClick()
        runOnIdle { component.saveState.value = ResumeSaveState.Saved }
        onNodeWithText("Resume saved successfully").assertExists()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("Resume saved successfully").fetchSemanticsNodes().isEmpty()
        }
        onNodeWithText("Save").performClick()
        runOnIdle { component.successfulSaves.value += 1 }
        onNodeWithText("Resume saved successfully").assertExists()
        onNodeWithTag("resume.name").performTextReplacement("Unsaved")
        onNodeWithText("Resume saved successfully").assertDoesNotExist()
        onNodeWithText("Close").performClick()
        assertEquals(2, writes)
        assertEquals(1, closes)
    }

    @Test
    fun feedbackDoesNotMoveFormOrControlsBetweenEmptySuccessAndError() = runDesktopComposeUiTest {
        val resume = Resume(id = 12, projectId = 1, name = "Mobile", createdAt = 0, updatedAt = 0)
        val component = TestResumeEditorComponent(initialResume = resume)
        setGoldenContent(this) { ResumeEditorContent(component) }
        val fields = onNodeWithTag("resume.fields").fetchSemanticsNode().boundsInRoot
        val feedback = onNodeWithTag("resume.feedback").fetchSemanticsNode().boundsInRoot
        val save = onNodeWithText("Save").fetchSemanticsNode().boundsInRoot
        listOf(
            ResumeSaveState.Saved,
            ResumeSaveState.Failed,
            ResumeSaveState.Idle,
        ).forEach { state ->
            runOnIdle { component.saveState.value = state }
            assertEquals(fields, onNodeWithTag("resume.fields").fetchSemanticsNode().boundsInRoot)
            assertEquals(
                feedback,
                onNodeWithTag("resume.feedback").fetchSemanticsNode().boundsInRoot,
            )
            assertEquals(save, onNodeWithText("Save").fetchSemanticsNode().boundsInRoot)
        }
    }

    @Test
    fun escapeClosesSavedResumeAfterHoveringActionsWithoutClicking() = runDesktopComposeUiTest {
        var closes = 0
        var writes = 0
        var deletes = 0
        val resume = Resume(id = 12, projectId = 1, name = "Mobile", createdAt = 0, updatedAt = 0)
        val component = TestResumeEditorComponent(
            initialResume = resume,
            close = { closes++ },
            save = { writes++ },
            delete = { deletes++ },
        )
        val resumes = TestResumesComponent(initialEditor = component, back = component::onClose)
        setGoldenContent(this) { RootContent(TestRootComponent(resumesComponent = resumes)) }
        onNodeWithText("Save").performMouseInput { enter(center) }
        onNodeWithText("Export").performMouseInput { moveTo(center) }
        onNodeWithText("Delete").performMouseInput { moveTo(center) }
        onNodeWithText("Close").performMouseInput { moveTo(center) }
        onNodeWithTag("resumes.editor").performKeyInput { pressKey(Key.Escape) }
        assertEquals(1, closes)
        assertEquals(0, writes)
        assertEquals(0, deletes)
        assertFalse(component.exportVisible.value)
    }
}
