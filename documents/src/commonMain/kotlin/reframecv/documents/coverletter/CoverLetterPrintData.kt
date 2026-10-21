package reframecv.documents.coverletter

import reframecv.documents.PrintLink

/** Supplies printable letter content independently of local identity and screen state. */
data class CoverLetterPrintData(val text: String, val links: List<PrintLink> = emptyList())
