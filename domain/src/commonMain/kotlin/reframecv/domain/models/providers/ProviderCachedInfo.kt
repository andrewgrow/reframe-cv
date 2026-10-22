package reframecv.domain.models.providers

import kotlinx.serialization.Serializable

/** Separates refreshable service information from user configuration. */
sealed interface ProviderCachedInfo

/** Caches OpenRouter account information independently of credentials. */
@Serializable
data class OpenRouterCachedInfo(
    val accountInfo: ProviderAccountInfo? = null,
    val creditBalance: ProviderCreditBalance? = null,
) : ProviderCachedInfo
