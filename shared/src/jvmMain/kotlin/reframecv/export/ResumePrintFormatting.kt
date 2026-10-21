package reframecv.export

import reframecv.documents.resume.ResumePrintItem
import reframecv.documents.resume.ResumePrintKind
import reframecv.domain.models.resume.content.ResumeContact
import reframecv.domain.models.resume.content.ResumeDate
import reframecv.domain.models.resume.content.ResumeDateRange
import reframecv.domain.models.resume.content.ResumeLink

internal fun joinPrint(vararg values: String, separator: String = " | ") =
    values.map(String::trim).filter(String::isNotEmpty).joinToString(separator)

internal fun printPeriod(period: ResumeDateRange): String = joinPrint(
    printDate(period.start),
    if (period.isCurrent) "Present" else printDate(period.end),
    separator = " - ",
)

private fun printDate(date: ResumeDate?): String = date?.let {
    listOfNotNull(it.year.toString(), it.month?.toString()?.padStart(2, '0')).joinToString("-")
}.orEmpty()

internal fun printLinks(links: List<ResumeLink>) = links.filter { it.url.isNotBlank() }.map {
    ResumePrintItem(it.label.ifBlank { it.url }, ResumePrintKind.Link, url = safePrintUrl(it.url))
}

internal fun contactUrl(contact: ResumeContact): String? = when {
    contact.type.equals(
        "email",
        ignoreCase = true,
    ) && '@' in contact.value -> "mailto:${contact.value.trim()}"

    contact.type.equals(
        "phone",
        ignoreCase = true,
    ) && contact.value.isNotBlank() -> "tel:${contact.value.trim()}"

    else -> safePrintUrl(contact.value)
}

private fun safePrintUrl(value: String): String? = value.trim().takeIf {
    it.startsWith("https://", true) || it.startsWith("http://", true) ||
        it.startsWith("mailto:", true) || it.startsWith("tel:", true)
}
