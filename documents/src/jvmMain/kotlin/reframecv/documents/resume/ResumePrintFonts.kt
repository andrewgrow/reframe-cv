package reframecv.documents.resume

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

/** Embeds the same licensed serif faces on every desktop instead of relying on system fonts. */
internal val ResumeSerif = FontFamily(
    Font("fonts/tinos/Tinos-Regular.ttf", FontWeight.Normal, FontStyle.Normal),
    Font("fonts/tinos/Tinos-Bold.ttf", FontWeight.Bold, FontStyle.Normal),
    Font("fonts/tinos/Tinos-Italic.ttf", FontWeight.Normal, FontStyle.Italic),
    Font("fonts/tinos/Tinos-BoldItalic.ttf", FontWeight.Bold, FontStyle.Italic),
)
