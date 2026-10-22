package reframecv.domain.models.providers

import kotlinx.serialization.Serializable

/** Defines supported configuration formats without exposing credentials in diagnostics. */
sealed interface ProviderConfiguration {
    val valid: Boolean
    fun matches(type: ApiProviderType): Boolean
}

/** Holds OpenRouter credentials and the selected AI model. */
@Serializable
data class OpenRouterConfiguration(
    val apiKey: String = "",
    val modelId: String = "",
    val managementApiKey: String = "",
    val version: Int = 1,
) : ProviderConfiguration {
    override val valid get() = version == 1 && apiKey.isNotBlank()
    override fun matches(type: ApiProviderType) = type == ApiProviderType.OpenRouter
    override fun toString() = "OpenRouterConfiguration(version=$version)"
}

/** Holds the credentials shared by Apify operations. */
@Serializable
data class ApifyConfiguration(val apiToken: String = "", val version: Int = 1) :
    ProviderConfiguration {
    override val valid get() = version == 1 && apiToken.isNotBlank()
    override fun matches(type: ApiProviderType) = type == ApiProviderType.Apify
    override fun toString() = "ApifyConfiguration(version=$version)"
}

/** Retains unsupported or damaged configuration for correction without executing it. */
data class InvalidProviderConfiguration(val raw: String) : ProviderConfiguration {
    override val valid = false
    override fun matches(type: ApiProviderType) = false
    override fun toString() = "InvalidProviderConfiguration"
}

fun ProviderConfiguration.normalized(): ProviderConfiguration = when (this) {
    is OpenRouterConfiguration -> copy(
        apiKey = apiKey.trim(),
        modelId = modelId.trim(),
        managementApiKey = managementApiKey.trim(),
    )

    is ApifyConfiguration -> copy(apiToken = apiToken.trim())

    is InvalidProviderConfiguration -> this
}
