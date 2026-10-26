package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import reframecv.logging.ApplicationLogger
import reframecv.runners.RunnerExecutionException

/** Reports remote errors without encouraging an accidental repeat of a paid start. */
internal fun runnerReadFailure(
    state: MutableValue<RunnerRunsState>,
    logger: ApplicationLogger,
    operation: String,
    error: Exception,
    starting: Boolean = false,
) {
    logger.error(operation, error, mapOf("runnerId" to state.value.runner.id))
    state.value = state.value.copy(
        starting = false,
        reading = false,
        error = (error as? RunnerExecutionException)?.details ?: if (starting) {
            "Could not confirm the start. Check Apify Console before starting another run."
        } else {
            "Could not read the run. Try refreshing."
        },
    )
}
