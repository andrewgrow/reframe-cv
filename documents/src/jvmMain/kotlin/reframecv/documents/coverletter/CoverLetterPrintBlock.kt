package reframecv.documents.coverletter

import androidx.compose.ui.unit.dp

/** Represents only the text fragment physically rendered on one page. */
internal data class CoverLetterPrintBlock(
    val text: String,
    val url: String? = null,
    val paragraphEnd: Boolean = true,
) {
    val gap get() = when {
        !paragraphEnd -> 0.dp
        url != null -> CoverLetterPrintStyle.linkGap
        else -> CoverLetterPrintStyle.paragraphGap
    }
}
