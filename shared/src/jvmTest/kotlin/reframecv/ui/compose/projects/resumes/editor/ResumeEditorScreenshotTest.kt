package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeSection
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.projects.resumes.TestResumesComponent
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.ui.components.projects.resumes.editor.ResumeRole
import reframecv.ui.components.projects.resumes.editor.ResumeSaveState
import reframecv.ui.components.projects.resumes.editor.ResumeSectionDraft
import reframecv.ui.components.projects.resumes.editor.TestResumeEditorComponent
import reframecv.ui.compose.application.RootContent
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode
import reframecv.ui.theme.UiScale

@OptIn(ExperimentalTestApi::class)
class ResumeEditorScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun mainResumeCreationMatchesReference() = runDesktopComposeUiTest {
        val component =
            TestResumeEditorComponent(ResumeDraft("Android Developer", role = ResumeRole.Main))
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }

    @Test
    fun editingPageMatchesReference() = runDesktopComposeUiTest {
        val resume = Resume(
            id = 12,
            projectId = 1,
            name = "Mobile developer",
            createdAt = 10,
            updatedAt = 20,
            content = sampleResumeContent(),
            keywords = listOf("Kotlin", "Compose"),
        )
        val component =
            TestResumeEditorComponent(initialResume = resume, initialRole = ResumeRole.Main)
        val root = TestRootComponent(
            resumesComponent = TestResumesComponent(initialEditor = component),
            initialTreeState = ProjectTreeState(
                projects = listOf(Project(id = 1, name = "Mobile", createdAt = 0, updatedAt = 0)),
                selectedId = 1,
                loading = false,
            ),
        )
        setGoldenContent(this) { RootContent(root) }
        onNodeWithText("Close").performClick()
        captureGolden(this)
    }

    @Test
    fun failedUpdateInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val resume = Resume(
            id = 12,
            projectId = 1,
            name = "Mobile developer",
            createdAt = 10,
            updatedAt = 20,
            content = sampleResumeContent(),
        )
        val component = TestResumeEditorComponent(initialResume = resume)
        component.saveState.value = ResumeSaveState.Failed
        setGoldenContent(this, themeMode = ThemeMode.Light) { ResumeEditorContent(component) }
        onNodeWithText("Close").performClick()
        captureGolden(this)
    }

    @Test
    fun successfulSaveMatchesReference() = runDesktopComposeUiTest {
        val resume = Resume(id = 12, projectId = 1, name = "Mobile", createdAt = 0, updatedAt = 0)
        val component = TestResumeEditorComponent(initialResume = resume)
        component.saveState.value = ResumeSaveState.Saved
        setGoldenContent(this) { ResumeEditorContent(component) }
        captureGolden(this)
    }

    @Test
    fun emptyCreationPageMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent()
        val root = TestRootComponent(
            resumesComponent = TestResumesComponent(initialEditor = component),
            initialTreeState = ProjectTreeState(
                projects = listOf(Project(id = 1, name = "Mobile", createdAt = 0, updatedAt = 0)),
                selectedId = 1,
                loading = false,
            ),
        )
        setGoldenContent(this) { RootContent(root) }
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }

    @Test
    fun missingNameInLightThemeMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent()
        setGoldenContent(this, themeMode = ThemeMode.Light) { ResumeEditorContent(component) }
        onNodeWithText("Save").performClick()
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }

    @Test
    fun failedSavePreservesProfileMatchesReference() = runDesktopComposeUiTest {
        val component = TestResumeEditorComponent(
            ResumeDraft("Mobile developer", profile = sampleResumeContent().profile),
        )
        component.saveState.value = ResumeSaveState.Failed
        setGoldenContent(this) { ResumeEditorContent(component) }
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }

    @Test
    fun narrowCreationMatchesReference() = runDesktopComposeUiTest(ScreenSize(320.dp, 600.dp)) {
        setGoldenContent(this, screenSize = ScreenSize(320.dp, 600.dp)) {
            ResumeEditorContent(TestResumeEditorComponent())
        }
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }

    @Test
    fun increasedScaleMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            ReframeTheme(themeMode = ThemeMode.Dark, uiScale = UiScale(150)) {
                ResumeEditorContent(TestResumeEditorComponent())
            }
        }
        onNodeWithText("Cancel").performClick()
        captureGolden(this)
    }

    @Test
    fun summarySectionMatchesReference() = captureSection(1)

    @Test
    fun skillsSectionInLightThemeMatchesReference() = captureSection(2, ThemeMode.Light)

    @Test
    fun experienceSectionMatchesReference() = captureSection(3)

    @Test
    fun educationSectionMatchesReference() = captureSection(4)

    @Test
    fun languagesSectionMatchesReference() = captureSection(5)

    @Test
    fun hiddenSoftSkillsSectionMatchesReference() = captureSection(6)

    @Test
    fun contactsSectionMatchesReference() = captureSection(0)

    @Test
    fun customTextSectionMatchesReference() = captureSection(7)

    private fun captureSection(index: Int, theme: ThemeMode = ThemeMode.Dark) =
        runDesktopComposeUiTest {
            val section: ResumeSection = sampleResumeContent().sections[index]
            setGoldenContent(this, themeMode = theme) {
                Column(
                    Modifier.fillMaxSize().background(
                        MaterialTheme.colorScheme.background,
                    ).verticalScroll(
                        rememberScrollState(),
                    ).padding(ReframeTheme.tokens.spacing.medium),
                ) {
                    ResumeSectionEditor(
                        ResumeSectionDraft(1, section),
                        ResumeFormScope(true),
                        {},
                        {},
                        {},
                        first = true,
                        last = true,
                    )
                }
            }
            captureGolden(this)
        }
}
