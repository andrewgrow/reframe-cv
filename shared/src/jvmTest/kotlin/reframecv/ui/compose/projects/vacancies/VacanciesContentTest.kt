package reframecv.ui.compose.projects.vacancies

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import reframecv.ui.components.projects.vacancies.TestVacanciesComponent
import reframecv.ui.components.projects.vacancies.VacanciesState
import reframecv.ui.compose.common.LOADING_INDICATOR_TAG
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class VacanciesContentTest {
    @Test
    fun showsVacancyDetailsAndKeepsControlsAnchoredWhileScrolling() = runComposeUiTest {
        var backs = 0
        val sample = vacancySamples().first()
        val component = TestVacanciesComponent(
            initialState = VacanciesState.Ready(
                List(30) {
                    sample.copy(id = it.toLong(), name = "Vacancy $it")
                },
            ),
            back = { backs++ },
        )
        setContent { ReframeTheme { VacanciesContent(component) } }
        onNodeWithText("Vacancy 0").assertIsDisplayed()
        onNodeWithTag(VACANCIES_LIST_TAG).performScrollToNode(hasText("Vacancy 29"))
        onNodeWithText("Vacancy 29").assertIsDisplayed()
        onNodeWithText("Add").assertIsDisplayed().performClick()
        onNodeWithText("Vacancy 29").assertIsDisplayed()
        assertEquals(0, backs)
        onNodeWithText("Back").assertIsDisplayed().performClick()
        assertEquals(1, backs)
        runOnIdle { component.uiState.value = VacanciesState.Ready(listOf(sample)) }
        onNodeWithText("2026-10-10").assertIsDisplayed()
        onNodeWithText("Example").assertIsDisplayed()
        onNodeWithText("Senior Android Developer").assertIsDisplayed()
    }

    @Test
    fun delayedLoadingFailureRetryAndEmptyState() = runComposeUiTest {
        mainClock.autoAdvance = false
        var retries = 0
        val component =
            TestVacanciesComponent(initialState = VacanciesState.Loading, retry = { retries++ })
        setContent { ReframeTheme { VacanciesContent(component) } }
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeBy(900)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        onNodeWithText("No vacancies yet.").assertDoesNotExist()
        onNodeWithText("Retry").assertDoesNotExist()
        mainClock.advanceTimeBy(200)
        onNodeWithTag(LOADING_INDICATOR_TAG).assertIsDisplayed()
        runOnIdle { component.uiState.value = VacanciesState.LoadFailed }
        mainClock.advanceTimeByFrame()
        onNodeWithTag(LOADING_INDICATOR_TAG).assertDoesNotExist()
        val addBounds = onNodeWithText("Add").fetchSemanticsNode().boundsInRoot
        val retryBounds = onNodeWithText("Retry").fetchSemanticsNode().boundsInRoot
        val backBounds = onNodeWithText("Back").fetchSemanticsNode().boundsInRoot
        assertTrue(addBounds.right < retryBounds.left && retryBounds.right < backBounds.left)
        assertEquals(addBounds.top, retryBounds.top)
        assertEquals(retryBounds.top, backBounds.top)
        onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
        runOnIdle { component.uiState.value = VacanciesState.Ready(emptyList()) }
        mainClock.advanceTimeByFrame()
        onNodeWithText("No vacancies yet.").assertIsDisplayed()
        onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun creationDateUsesRequestedTimeZoneIncludingDateBoundary() {
        val instant = Instant.parse("2026-10-10T00:30:00Z").toEpochMilliseconds()
        assertEquals("2026-10-10", vacancyDate(instant, TimeZone.UTC))
        assertEquals("2026-10-09", vacancyDate(instant, TimeZone.of("America/Los_Angeles")))
        assertEquals("2026-10-10", vacancyDate(instant, TimeZone.of("Europe/Warsaw")))
    }
}
