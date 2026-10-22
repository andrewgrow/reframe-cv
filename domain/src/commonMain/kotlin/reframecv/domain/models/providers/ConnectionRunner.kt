package reframecv.domain.models.providers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import reframecv.domain.DomainModel

/** Identifies the adapter responsible for interpreting an operation's inputs and outputs. */
enum class ConnectionOperation { LinkedInJobSearch, }

/** Stores a reusable operation independently of individual executions. */
data class ConnectionRunner(
    val id: Long = 0,
    val connectionId: Long,
    val name: String,
    val operation: ConnectionOperation,
    val configuration: String,
    val active: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) : DomainModel {
    override fun toString() = "ConnectionRunner(id=$id, connectionId=$connectionId)"
}

/** Defines the first supported Apify operation without embedding connection credentials. */
@Serializable
data class LinkedInJobSearchConfiguration(
    val actorId: String = "curious_coder/linkedin-jobs-scraper",
    val searchUrls: List<String> = emptyList(),
    val maxResults: Int = 100,
    val version: Int = 1,
) {
    val valid get() = version == 1 && actorId == "curious_coder/linkedin-jobs-scraper" &&
        maxResults > 0 && searchUrls.isNotEmpty() && searchUrls.all { it.isNotBlank() }
}

/** Validates supported runner formats before a remote execution can be created. */
object RunnerConfigurationJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    fun encode(value: LinkedInJobSearchConfiguration) = json.encodeToString(value)
    fun decode(runner: ConnectionRunner): LinkedInJobSearchConfiguration? = try {
        when (runner.operation) {
            ConnectionOperation.LinkedInJobSearch ->
                json.decodeFromString<LinkedInJobSearchConfiguration>(runner.configuration)
                    .takeIf { it.valid }
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}
