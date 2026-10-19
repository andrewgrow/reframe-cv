package reframecv.ui.compose.application

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import reframecv.domain.models.project.Project
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.application.navigation.TestProjectTreeComponent
import reframecv.ui.components.common.section.CollapsibleSectionState
import reframecv.ui.components.common.section.TestCollapsibleSectionComponent
import reframecv.ui.theme.ReframeTheme

@OptIn(ExperimentalTestApi::class)
class NavigationCollapseTest {
    @Test
    fun restoresCollapsedWidthWithoutAnimatingAtStartup() = checkRestoration(false)

    @Test
    fun restoresExpandedWidthWithoutAnimatingAtStartup() = checkRestoration(true)

    private fun checkRestoration(expanded: Boolean) = runDesktopComposeUiTest {
        val panel = TestCollapsibleSectionComponent(CollapsibleSectionState(loading = true))
        val tree = TestProjectTreeComponent()
        setContent { ReframeTheme { RootNavigation(tree, panel) } }
        onNodeWithTag("navigation.panel").assertDoesNotExist()
        mainClock.autoAdvance = false
        runOnIdle { panel.state.value = CollapsibleSectionState(expanded = expanded) }
        mainClock.advanceTimeByFrame()
        val restoredWidth = onNodeWithTag("navigation.panel").fetchSemanticsNode().size.width
        mainClock.advanceTimeBy(400)
        assertEquals(
            restoredWidth,
            onNodeWithTag("navigation.panel").fetchSemanticsNode().size.width,
        )
        onNodeWithTag(if (expanded) "navigation.collapse" else "navigation.expand").performClick()
        mainClock.advanceTimeBy(100)
        val movingWidth = onNodeWithTag("navigation.panel").fetchSemanticsNode().size.width
        kotlin.test.assertTrue(
            if (expanded) {
                movingWidth < restoredWidth
            } else {
                movingWidth >
                    restoredWidth
            },
        )
        mainClock.autoAdvance = true
    }

    @Test
    fun collapsingHidesTextKeepsSettingsAndRestoresSelectedTree() = runDesktopComposeUiTest {
        val tree = TestProjectTreeComponent(
            ProjectTreeState(
                projects = listOf(
                    Project(1, "Backend", 0, 0),
                    Project(2, "Java", 0, 0, parentId = 1),
                ),
                selectedId = 2,
                expandedIds = setOf(1),
                loading = false,
            ),
        )
        var settings by mutableStateOf(false)
        setContent {
            ReframeTheme {
                RootNavigation(tree, remember { TestCollapsibleSectionComponent() }, onSettings = {
                    settings =
                        true
                }, settingsSelected = settings)
            }
        }
        onNodeWithText("Java").assertIsSelected()
        val width = onNodeWithTag("navigation.panel").fetchSemanticsNode().size.width
        val gearWidth = onNodeWithTag("navigation.settings").fetchSemanticsNode().size.width
        onNodeWithContentDescription("Collapse navigation").performClick()
        waitForIdle()
        onNodeWithText("Projects").assertDoesNotExist()
        onNodeWithText("Java").assertDoesNotExist()
        onNodeWithText("Settings").assertDoesNotExist()
        onNodeWithContentDescription("Expand navigation").assertIsDisplayed()
        val panelWidth = onNodeWithTag("navigation.panel").fetchSemanticsNode().size.width
        val gear = onNodeWithTag("navigation.settings").fetchSemanticsNode().boundsInRoot
        assertEquals(gear.width + gear.left * 2, panelWidth.toFloat(), absoluteTolerance = 1f)
        kotlin.test.assertTrue(panelWidth < width && gear.width < gearWidth)
        onNodeWithContentDescription("Settings").performClick()
        onNodeWithTag("navigation.settings").assertIsSelected()
        onNodeWithContentDescription("Expand navigation").performClick()
        waitForIdle()
        assertEquals(width, onNodeWithTag("navigation.panel").fetchSemanticsNode().size.width)
        onNodeWithText("Settings").assertIsDisplayed()
        onNodeWithText("Java").assertIsDisplayed()
        assertEquals(2L, tree.state.value.selectedId)
        assertEquals(setOf(1L), tree.state.value.expandedIds)
    }
}
