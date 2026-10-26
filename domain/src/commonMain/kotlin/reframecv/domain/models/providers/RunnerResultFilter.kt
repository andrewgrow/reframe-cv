package reframecv.domain.models.providers

/** Selects local review rows, including decisions remembered from earlier runs. */
data class RunnerResultFilter(
    val showDismissed: Boolean = true,
    val showImported: Boolean = true,
) {
    fun includes(result: RunnerResult): Boolean = when (result.reviewStatus) {
        RunnerResultStatus.Dismissed -> showDismissed
        RunnerResultStatus.Imported -> showImported
        RunnerResultStatus.New, RunnerResultStatus.Restored -> true
    }
}
