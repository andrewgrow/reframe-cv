package reframecv.ui.compose.projects.resumes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.domain.models.resume.content.ResumeLink
import reframecv.domain.models.resume.content.sampleResumeContent
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_position_links
import reframecv.shared.generated.resources.resume_project_links
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ResumeExtendedFieldsScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun profileWorkDetailsMatchReference() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .padding(ReframeTheme.tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                ) {
                    ResumeFormScope(true).ProfileFields(sampleResumeContent().profile) {}
                }
            }
        }
        captureGolden(this)
    }

    @Test
    fun positionAndProjectLinksMatchReference() = captureLinks(ThemeMode.Dark, enabled = true)

    @Test
    fun disabledLinksInLightThemeMatchReference() = captureLinks(ThemeMode.Light, enabled = false)

    private fun captureLinks(theme: ThemeMode, enabled: Boolean) = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = theme) {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                    .padding(ReframeTheme.tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(ReframeTheme.tokens.spacing.medium),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground,
                ) {
                    val form = ResumeFormScope(enabled)
                    form.LinkFields(
                        listOf(ResumeLink("Google Play", "https://example.com/app")),
                        Res.string.resume_position_links,
                    ) {}
                    form.LinkFields(emptyList(), Res.string.resume_project_links) {}
                }
            }
        }
        captureGolden(this)
    }
}
