package reframecv.ui.compose.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import reframecv.ui.theme.ReframeTheme

private const val LOADING_INDICATOR_DELAY_MILLIS = 1_000L
internal const val LOADING_INDICATOR_TAG = "loading.indicator"

/** Leaving the composition cancels the delay; each new loading state starts it again. */
@Composable
internal fun LoadingContent(modifier: Modifier = Modifier) {
    var showIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_INDICATOR_DELAY_MILLIS.milliseconds)
        showIndicator = true
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showIndicator) {
            CircularProgressIndicator(
                modifier = Modifier.size(ReframeTheme.tokens.dimensions.loadingIndicatorSize)
                    .testTag(LOADING_INDICATOR_TAG),
            )
        }
    }
}
