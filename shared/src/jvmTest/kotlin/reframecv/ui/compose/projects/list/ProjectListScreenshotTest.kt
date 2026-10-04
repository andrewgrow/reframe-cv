package reframecv.ui.compose.projects.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import reframecv.domain.models.project.Project
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ProjectListScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun inDarkThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Dark)
    }

    @Test
    fun inLightThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Light)
    }

    @Test
    fun hoveredRowMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Dark, hover = true)
    }

    @Test
    fun hoveredRowInLightThemeMatchesReference() = runComposeUiTest {
        captureComponent(this, ThemeMode.Light, hover = true)
    }

    @Test
    fun focusedRowMatchesReference() = runComposeUiTest {
        setListContent(this, ThemeMode.Dark)
        onNodeWithTag(projectRowTag(2))
            .performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        captureGolden(this)
    }

    private fun captureComponent(
        test: ComposeUiTest,
        themeMode: ThemeMode,
        hover: Boolean = false,
    ) {
        setListContent(test, themeMode)
        if (hover) {
            test.onNodeWithTag(projectRowTag(2)).performMouseInput { enter(center) }
        }
        captureGolden(test)
    }

    private fun setListContent(test: ComposeUiTest, themeMode: ThemeMode) {
        setGoldenContent(test, themeMode = themeMode) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                ProjectList(
                    projects = List(30) { index ->
                        Project(
                            id = index.toLong(),
                            name = if (index == 0) "Android Developer" else "Resume project $index",
                            createdAt = 0,
                            updatedAt = 0,
                        )
                    },
                    onEditProject = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
