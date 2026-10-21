package reframecv.ui.compose.projects.vacancies

/** Keeps checkbox interaction separate from opening a vacancy. */
internal data class VacancyRowSelection(
    val checked: Boolean,
    val enabled: Boolean,
    val toggle: () -> Unit,
)
