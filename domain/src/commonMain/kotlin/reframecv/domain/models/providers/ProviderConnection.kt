package reframecv.domain.models.providers

import reframecv.domain.DomainModel

/** Identifies provider implementations installed in the application. */
enum class ApiProviderType { OpenRouter, Apify, OpenAI }

/** Represents access to a service independently of its provider-specific configuration. */
data class ProviderConnection(
    val id: Long = 0,
    val type: ApiProviderType,
    val name: String,
    val configuration: ProviderConfiguration,
    val active: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val cachedInfo: ProviderCachedInfo? = null,
) : DomainModel {
    val usable: Boolean get() = configuration.matches(type) && configuration.valid
    val ai: AiProviderConfiguration get() = configuration as AiProviderConfiguration
    val openRouter: OpenRouterConfiguration get() = configuration as OpenRouterConfiguration
    val openRouterCache: OpenRouterCachedInfo get() =
        cachedInfo as? OpenRouterCachedInfo ?: OpenRouterCachedInfo()
    override fun toString() = "ProviderConnection(id=$id, type=$type, active=$active)"
}

/** Describes a selectable model returned by an AI provider. */
data class AiModel(val id: String, val name: String, val structuredOutput: Boolean = false)
