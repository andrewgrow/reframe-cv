package reframecv.documents.resume

import reframecv.documents.PrintLink

/** Supplies already filtered resume content without database or editor dependencies. */
data class ResumePrintData(
    val header: List<ResumePrintItem>,
    val items: List<ResumePrintItem>,
    val photo: ByteArray? = null,
    val contacts: List<PrintLink> = emptyList(),
)

/** Identifies typography and page-break rules for one printable resume block. */
data class ResumePrintItem(
    val text: String,
    val kind: ResumePrintKind = ResumePrintKind.Body,
    val url: String? = null,
    val keepWithNext: Boolean = false,
    val paragraphEnd: Boolean = true,
    val ruleBefore: Boolean = false,
    val boldPrefixLength: Int = 0,
    val italicSuffixStart: Int? = null,
    val continuation: Boolean = false,
)

/** Keeps semantic print typography separate from application theme tokens. */
enum class ResumePrintKind {
    Name,
    Headline,
    Section,
    Subheading,
    Role,
    Body,
    Detail,
    Bullet,
    SummaryLead,
    Link,
    Consent,
}
