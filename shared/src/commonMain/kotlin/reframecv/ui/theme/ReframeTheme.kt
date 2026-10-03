package reframecv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private const val HINT_COLOR_LIGHT = 0xFF62606A
private const val HINT_COLOR_DARK = 0xFFB9B2C3
private const val BACKGROUND_COLOR_DARK = 0xFF211D29
private const val CRITICAL_COLOR_LIGHT = 0xFFB3261E
private const val CRITICAL_COLOR_DARK = 0xFFF2B8B5

val hintColorLight = Color(HINT_COLOR_LIGHT)
val hintColorDark = Color(HINT_COLOR_DARK)
val criticalColorLight = Color(CRITICAL_COLOR_LIGHT)
val criticalColorDark = Color(CRITICAL_COLOR_DARK)

/** Material colors plus application-specific semantic colors. */
@Immutable
data class ReframeColorScheme(val material: ColorScheme, val hint: Color, val critical: Color)

private val lightReframeColorScheme = ReframeColorScheme(
    material = lightColorScheme().run {
        copy(background = primaryContainer, onBackground = onPrimaryContainer)
    },
    hint = hintColorLight,
    critical = criticalColorLight,
)
private val darkReframeColorScheme = ReframeColorScheme(
    material = darkColorScheme(background = Color(BACKGROUND_COLOR_DARK)),
    hint = hintColorDark,
    critical = criticalColorDark,
)
private val LocalReframeColorScheme = staticCompositionLocalOf { lightReframeColorScheme }

object ReframeTheme {
    sealed interface ThemeMode {
        data object Dark : ThemeMode
        data object Light : ThemeMode
    }

    val colorScheme: ReframeColorScheme
        @Composable get() = LocalReframeColorScheme.current

    @Composable
    fun systemThemeMode(): ThemeMode = if (isSystemInDarkTheme()) {
        ThemeMode.Dark
    } else {
        ThemeMode.Light
    }
}

@Composable
fun ReframeTheme(
    themeMode: ReframeTheme.ThemeMode = ReframeTheme.systemThemeMode(),
    content: @Composable () -> Unit,
) {
    val colors = when (themeMode) {
        ReframeTheme.ThemeMode.Dark -> darkReframeColorScheme
        ReframeTheme.ThemeMode.Light -> lightReframeColorScheme
    }
    CompositionLocalProvider(LocalReframeColorScheme provides colors) {
        MaterialTheme(colorScheme = colors.material, content = content)
    }
}
