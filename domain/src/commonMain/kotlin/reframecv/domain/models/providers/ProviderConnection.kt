package reframecv.domain.models.providers

import reframecv.domain.DomainModel

/** Identifies API implementations available in this application. */
enum class ApiProviderType { OpenRouter, }

/** Represents a user-configured API connection independently of its storage. */
data class ProviderConnection(
    val id: Long = 0,
    val type: ApiProviderType,
    val name: String,
    val apiKey: String,
    val modelId: String,
    val active: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val accountInfo: ProviderAccountInfo? = null,
    val managementApiKey: String = "",
    val creditBalance: ProviderCreditBalance? = null,
) : DomainModel {
    override fun toString() = "ProviderConnection(id=$id, type=$type, name=$name, modelId=$modelId)"
}

/** Describes a selectable model returned by a provider. */
data class AiModel(val id: String, val name: String, val structuredOutput: Boolean = false)
