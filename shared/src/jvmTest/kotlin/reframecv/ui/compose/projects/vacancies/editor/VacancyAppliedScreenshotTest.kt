package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft
import reframecv.ui.components.projects.vacancies.editor.appliedTimeText
import reframecv.ui.components.projects.vacancies.editor.parseAppliedTime
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancyAppliedScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun uncheckedApplicationKeepsTimeControlsDisabled() = runComposeUiTest {
        setAndCaptureGolden(this, ScreenSize(600.dp, 180.dp)) {
            AppliedPreview(VacancyDraft("Job"), {}, true)
        }
        onNodeWithTag("vacancy.applied").assertIsOff()
        onNodeWithTag("vacancy.applied.at").assertIsNotEnabled()
        onNodeWithTag("vacancy.applied.now").assertIsNotEnabled()
    }

    @Test
    fun manualAppliedTimestampInLightTheme() = runComposeUiTest {
        setAndCaptureGolden(this, ScreenSize(600.dp, 180.dp), themeMode = ThemeMode.Light) {
            AppliedPreview(
                VacancyDraft(
                    "Job",
                    applied = true,
                    appliedDate = "2026-10-21 18:45",
                ),
                {
                },
                true,
            )
        }
    }

    @Test
    fun invalidAppliedTimestampShowsFieldError() = runComposeUiTest {
        setAndCaptureGolden(this, ScreenSize(600.dp, 180.dp)) {
            AppliedPreview(
                VacancyDraft(
                    "Job",
                    applied = true,
                    appliedDate = "2026-02-30 18:45",
                ),
                {
                },
                true,
            )
        }
    }

    @Test
    fun setNowAndManualEditsOnlyChangeTheDraft() = runComposeUiTest {
        val draft = mutableStateOf(VacancyDraft("Job"))
        setGoldenContent(this, ScreenSize(600.dp, 180.dp)) {
            AppliedPreview(draft.value, { draft.value = it }, true)
        }
        onNodeWithTag("vacancy.applied").performClick().assertIsOn()
        val before = appliedTimeText()
        onNodeWithTag("vacancy.applied.now").performClick()
        waitForIdle()
        assertTrue(draft.value.appliedDate in setOf(before, appliedTimeText()))
        onNodeWithTag("vacancy.applied.at").performTextReplacement("2026-10-21 09:15")
        waitForIdle()
        assertEquals("2026-10-21 09:15", draft.value.appliedDate)
        onNodeWithTag("vacancy.applied").performClick()
        waitForIdle()
        assertEquals("2026-10-21 09:15", draft.value.appliedDate)
    }

    @Test
    fun appliedFieldsRemainVisibleAtTheBottomOfTheVacancyPage() = runComposeUiTest {
        val component = TestVacancyEditorComponent(
            initialVacancy = Vacancy(
                id = 1,
                projectId = 1,
                name = "ExampleCo mobile developer",
                createdAt = 1,
                updatedAt = 1,
                appliedAt = parseAppliedTime("2026-10-21 18:45"),
            ),
        )
        setGoldenContent(this) { VacancyEditorContent(component) }
        onNodeWithTag("vacancy.applied.at").performScrollTo()
        captureGolden(this)
    }

    @Composable
    private fun AppliedPreview(
        draft: VacancyDraft,
        change: (VacancyDraft) -> Unit,
        enabled: Boolean,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Box(Modifier.padding(ReframeTheme.tokens.spacing.medium)) {
                VacancyAppliedFields(draft, change, enabled)
            }
        }
    }
}
