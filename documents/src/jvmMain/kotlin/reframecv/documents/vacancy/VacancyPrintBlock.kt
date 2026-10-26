package reframecv.documents.vacancy

/** Keeps printable text fragments and page-break rules separate from the application model. */
internal data class VacancyPrintBlock(
    val text: String,
    val kind: VacancyPrintKind = VacancyPrintKind.Body,
    val url: String? = null,
    val paragraphEnd: Boolean = true,
    val continuation: Boolean = false,
) {
    val keepWithNext get() = kind == VacancyPrintKind.Section
}

/** Identifies typography and indentation for the vacancy template. */
internal enum class VacancyPrintKind { Title, Company, Link, Section, Body, Bullet }

internal fun VacancyPrintData.blocks(): List<VacancyPrintBlock> = buildList {
    add(VacancyPrintBlock(title.trim(), VacancyPrintKind.Title))
    if (company.isNotBlank()) add(VacancyPrintBlock(company.trim(), VacancyPrintKind.Company))
    if (url.isNotBlank()) add(VacancyPrintBlock(url.trim(), VacancyPrintKind.Link, url.trim()))
    val paragraphs = description.replace("\r\n", "\n").split(Regex("\n[\t ]*\n"))
        .filter(String::isNotBlank).map { VacancyPrintBlock(it.trim()) }
    section("Description", paragraphs)
    section("Responsibilities", responsibilities.bullets())
    section("Requirements", requirements.bullets())
    section("Nice-to-have", niceToHave.bullets())
}

private fun List<String>.bullets() = filter(String::isNotBlank).map {
    VacancyPrintBlock(it.trim(), VacancyPrintKind.Bullet)
}

private fun MutableList<VacancyPrintBlock>.section(title: String, blocks: List<VacancyPrintBlock>) {
    if (blocks.isNotEmpty()) {
        add(VacancyPrintBlock(title, VacancyPrintKind.Section))
        addAll(blocks)
    }
}
