package reframecv.ui.components.settings.runners

import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.LinkedInJobSearchConfiguration
import reframecv.domain.models.providers.RunnerConfigurationJson

/** Holds editable search inputs without losing partially entered numeric values. */
data class RunnerDraft(
    val name: String = "",
    val active: Boolean = true,
    val searchUrls: String = "",
    val maxResults: String = "100",
) {
    val urls: List<String> get() = searchUrls.lines().map { it.trim() }.filter { it.isNotEmpty() }
        .distinct()
    val validUrls: Boolean get() = urls.isNotEmpty() && urls.all(::isLinkedInSearchUrl)
    val validLimit: Boolean get() = (maxResults.trim().toIntOrNull() ?: 0) > 0
    val valid: Boolean get() = validUrls && validLimit
    fun applyTo(runner: ConnectionRunner): ConnectionRunner = runner.copy(
        name = name.trim(),
        active = active,
        configuration = RunnerConfigurationJson.encode(
            LinkedInJobSearchConfiguration(
                searchUrls = urls,
                maxResults = checkNotNull(maxResults.trim().toIntOrNull()),
            ),
        ),
    )
    companion object {
        fun from(runner: ConnectionRunner): RunnerDraft {
            val configuration = RunnerConfigurationJson.decode(runner)
            return RunnerDraft(
                runner.name,
                runner.active,
                configuration?.searchUrls?.joinToString("\n").orEmpty(),
                configuration?.maxResults?.toString() ?: "100",
            )
        }
        fun newRunner(connectionId: Long) = ConnectionRunner(
            connectionId = connectionId,
            name = "",
            operation = ConnectionOperation.LinkedInJobSearch,
            configuration = RunnerConfigurationJson.encode(LinkedInJobSearchConfiguration()),
        )
    }
}

private fun isLinkedInSearchUrl(value: String) = Regex(
    "^https://(?:[a-z]{2,3}\\.)?linkedin\\.com/jobs/search(?:[/?][^\\s]*)?$",
    RegexOption.IGNORE_CASE,
).matches(value)
