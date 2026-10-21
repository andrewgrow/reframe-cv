package reframecv.export

import reframecv.documents.PrintLink
import reframecv.documents.resume.ResumePrintData
import reframecv.documents.resume.ResumePrintItem
import reframecv.documents.resume.ResumePrintKind
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.resume.content.ResumeContactsSection

/** Applies print visibility before any resume data or image reaches the renderer. */
internal fun Resume.toPrintData(photo: ByteArray?): ResumePrintData {
    val profile = content.profile
    val header = listOf(
        ResumePrintItem(profile.name, ResumePrintKind.Name),
        ResumePrintItem(
            joinPrint(
                profile.location,
                profile.workAuthorization,
                profile.workArrangement,
                separator = " / ",
            ),
            ResumePrintKind.Detail,
        ),
        ResumePrintItem(profile.headline, ResumePrintKind.Headline),
    ).filter { it.text.isNotBlank() }
    val sections = content.sections.filter { it.visible }
    val contacts = sections.filterIsInstance<ResumeContactsSection>().flatMap { it.entries }
        .filter {
            it.value.isNotBlank()
        }.map { PrintLink(it.value.trim(), contactUrl(it).orEmpty()) }
    val items = sections.filterNot { it is ResumeContactsSection }
        .flatMap(::printSection).toMutableList()
    if (profile.headline.isNotBlank() && items.isNotEmpty()) {
        items[0] = items.first().copy(ruleBefore = true)
    }
    if (content.consent.visible && content.consent.text.isNotBlank()) {
        items.add(ResumePrintItem(content.consent.text, ResumePrintKind.Consent))
    }
    return ResumePrintData(header, items, photo.takeIf { photoVisible }, contacts)
}
