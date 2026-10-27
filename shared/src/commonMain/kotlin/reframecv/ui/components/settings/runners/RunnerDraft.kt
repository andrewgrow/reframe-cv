package reframecv.ui.components.settings.runners

import reframecv.domain.models.providers.AmsJobSearchConfiguration
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
    val operation: ConnectionOperation = ConnectionOperation.LinkedInJobSearch,
    val query: String = "",
    val location: String = "Wien",
    val radiusKm: String = "50",
    val includeExternalJobs: Boolean = true,
    val postedWithin: String = "",
) {
    val urls: List<String> get() = searchUrls.lines().map { it.trim() }.filter { it.isNotEmpty() }
        .distinct()
    val validUrls: Boolean get() = urls.isNotEmpty() && urls.all(::isLinkedInSearchUrl)
    val validLimit: Boolean get() = (maxResults.trim().toIntOrNull() ?: 0) in
        1..if (operation == ConnectionOperation.AmsJobSearch) 10000 else Int.MAX_VALUE
    val validRadius: Boolean get() = radiusKm.trim().toIntOrNull() in
        setOf(0, 5, 10, 20, 50, 75, 100)
    val valid: Boolean get() = validLimit && when (operation) {
        ConnectionOperation.LinkedInJobSearch -> validUrls
        ConnectionOperation.AmsJobSearch -> validAms
    }
    private val validAms get() = query.isNotBlank() && validRadius &&
        (radiusKm.trim() == "0" || location.isNotBlank()) &&
        postedWithin in setOf("", "LAST_WEEK", "LAST_MONTH")
    fun applyTo(runner: ConnectionRunner): ConnectionRunner = runner.copy(
        name = name.trim(),
        active = active,
        operation = operation,
        configuration = RunnerConfigurationJson.encode(
            when (operation) {
                ConnectionOperation.LinkedInJobSearch -> LinkedInJobSearchConfiguration(
                    searchUrls = urls,
                    maxResults = checkNotNull(maxResults.trim().toIntOrNull()),
                )

                ConnectionOperation.AmsJobSearch -> AmsJobSearchConfiguration(
                    query = query.trim(),
                    location = location.trim(),
                    radiusKm = checkNotNull(radiusKm.trim().toIntOrNull()),
                    maxResults = checkNotNull(maxResults.trim().toIntOrNull()),
                    includeExternalJobs = includeExternalJobs,
                    postedWithin = postedWithin,
                )
            },
        ),
    )
    companion object {
        fun from(runner: ConnectionRunner): RunnerDraft {
            val configuration = RunnerConfigurationJson.decode(runner)
            val draft = RunnerDraft(
                name = runner.name,
                active = runner.active,
                operation = runner.operation,
            )
            return when (configuration) {
                is LinkedInJobSearchConfiguration -> draft.copy(
                    searchUrls = configuration.searchUrls.joinToString("\n"),
                    maxResults = configuration.maxResults.toString(),
                )

                is AmsJobSearchConfiguration -> draft.copy(
                    query = configuration.query,
                    location = configuration.location,
                    radiusKm = configuration.radiusKm.toString(),
                    maxResults = configuration.maxResults.toString(),
                    includeExternalJobs = configuration.includeExternalJobs,
                    postedWithin = configuration.postedWithin,
                )

                null -> draft
            }
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
