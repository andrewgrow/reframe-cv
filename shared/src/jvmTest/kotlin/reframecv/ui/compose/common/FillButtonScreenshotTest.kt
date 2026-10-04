package reframecv.ui.compose.common

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertTrue
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class FillButtonScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun galleryInDarkThemeMatchesReference() = runComposeUiTest {
        captureGallery(this, ThemeMode.Dark)
    }

    @Test
    fun galleryInLightThemeMatchesReference() = runComposeUiTest {
        captureGallery(this, ThemeMode.Light)
    }

    @Test
    fun outlinedGalleryInDarkThemeMatchesReference() = runComposeUiTest {
        captureGallery(this, ThemeMode.Dark, FillButton.OutlineState.Visible)
    }

    @Test
    fun outlinedGalleryInLightThemeMatchesReference() = runComposeUiTest {
        captureGallery(this, ThemeMode.Light, FillButton.OutlineState.Visible)
    }

    private fun captureGallery(
        test: ComposeUiTest,
        themeMode: ThemeMode,
        outlineState: FillButton.OutlineState = FillButton.OutlineState.Hidden,
    ) {
        val sources = List(GalleryState.entries.size) { List(3) { MutableInteractionSource() } }
        setGoldenContent(test, themeMode = themeMode) { ButtonGallery(sources, outlineState) }
        test.waitForIdle()
        test.runOnIdle {
            GalleryState.entries.forEach { state ->
                sources[state.ordinal].forEach { source ->
                    when (state) {
                        GalleryState.Hover -> assertTrue(source.tryEmit(HoverInteraction.Enter()))

                        GalleryState.Focus -> assertTrue(source.tryEmit(FocusInteraction.Focus()))

                        GalleryState.Press -> assertTrue(
                            source.tryEmit(PressInteraction.Press(Offset.Zero)),
                        )

                        else -> Unit
                    }
                }
            }
        }
        test.waitForIdle()
        test.mainClock.autoAdvance = false
        test.runOnIdle {
            sources[GalleryState.Partial.ordinal].forEach { source ->
                assertTrue(source.tryEmit(HoverInteraction.Enter()))
            }
        }
        test.mainClock.advanceTimeByFrame()
        test.mainClock.advanceTimeByFrame()
        test.mainClock.advanceTimeBy(100)
        captureGolden(test)
    }
}

private enum class GalleryState(val label: String) {
    Idle("Idle"),
    Hover("Hovered"),
    Focus("Keyboard focus"),
    Press("Pressed"),
    Disabled("Disabled"),
    Partial("Filling · 100 ms"),
}

@Composable
private fun ButtonGallery(
    sources: List<List<MutableInteractionSource>>,
    outlineState: FillButton.OutlineState,
) {
    val colors = MaterialTheme.colorScheme
    val backgrounds = listOf(colors.background, colors.surfaceContainer, colors.secondaryContainer)
    val foregrounds = listOf(colors.onBackground, colors.onSurface, colors.onSecondaryContainer)
    val titles = listOf("App background", "Surface container", "Secondary container")
    val labels = listOf("Projects List", "Create resume", "Adapt resume to vacancy")

    Column(
        Modifier.fillMaxSize().background(colors.background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FillButton", color = colors.onBackground, style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            backgrounds.forEachIndexed { index, background ->
                Column(
                    Modifier.weight(1f).background(background).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        titles[index],
                        color = foregrounds[index],
                        style = MaterialTheme.typography.labelLarge,
                    )
                    GalleryState.entries.forEach { state ->
                        Column {
                            Text(
                                state.label,
                                color = foregrounds[index],
                                style = MaterialTheme.typography.labelMedium,
                            )
                            FillButton(
                                text = labels[index],
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state != GalleryState.Disabled,
                                outlineState = outlineState,
                                containerColor = background,
                                contentColor = foregrounds[index],
                                fillColor = if (index == 2) colors.tertiary else colors.primary,
                                filledContentColor = if (index == 2) {
                                    colors.onTertiary
                                } else {
                                    colors.onPrimary
                                },
                                interactionSource = sources[state.ordinal][index],
                            )
                        }
                    }
                }
            }
        }
    }
}
