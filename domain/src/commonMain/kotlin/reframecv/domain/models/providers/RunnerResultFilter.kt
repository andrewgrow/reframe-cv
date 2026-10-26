package reframecv.domain.models.providers

/** Selects local review rows, including decisions remembered from earlier runs. */
data class RunnerResultFilter(
    val showDismissed: Boolean = true,
    val showImported: Boolean = true,
) {
    fun includes(result: RunnerResult): Boolean {
        val status = if (result.status == RunnerResultStatus.New) {
            result.previousStatus ?: result.status
        } else {
            result.status
        }
        return when (status) {
            RunnerResultStatus.Dismissed -> showDismissed
            RunnerResultStatus.Imported -> showImported
            RunnerResultStatus.New -> true
        }
    }
}
