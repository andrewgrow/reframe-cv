package reframecv.providers.openrouter

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderBudget
import reframecv.shared.nowMillis

/** Reads account metadata without retaining provider-specific identifiers or credentials. */
@Serializable
internal data class OpenRouterAccountResponse(val data: OpenRouterAccountData)

/** Maps current budgets and older key-limit responses to the shared account model. */
@Serializable
internal data class OpenRouterAccountData(
    val budgets: List<OpenRouterBudget>? = null,
    @SerialName("effective_budget") val effectiveBudget: OpenRouterBudget? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val limit: Double? = null,
    @SerialName("limit_remaining") val remaining: Double? = null,
    @SerialName("limit_reset") val reset: String? = null,
    val usage: Double? = null,
    @SerialName("usage_daily") val dailyUsage: Double? = null,
    @SerialName("usage_weekly") val weeklyUsage: Double? = null,
    @SerialName("usage_monthly") val monthlyUsage: Double? = null,
) {
    fun toDomain(): ProviderAccountInfo {
        val parsed = budgets.orEmpty().map { it.toDomain() }
        val keyBudget = if (parsed.none { it.scope == "key_limit" } &&
            listOf(limit, remaining, usage).any { it != null }
        ) {
            listOf(
                ProviderBudget("key_limit", limit, periodUsage(), remaining, resetInterval = reset),
            )
        } else {
            emptyList()
        }
        return ProviderAccountInfo(
            budgets = parsed + keyBudget,
            effectiveBudget = effectiveBudget?.toDomain(),
            expiresAt = expiresAt?.let { Instant.parse(it).toEpochMilliseconds() },
            fetchedAt = nowMillis(),
        )
    }

    private fun periodUsage() = when (reset) {
        "daily" -> dailyUsage
        "weekly" -> weeklyUsage
        "monthly" -> monthlyUsage
        else -> usage
    }
}

/** Preserves the scope and reset date of a single OpenRouter spending constraint. */
@Serializable
internal data class OpenRouterBudget(
    val scope: String,
    @SerialName("limit_usd") val limit: Double? = null,
    @SerialName("spend_usd") val spend: Double? = null,
    @SerialName("remaining_usd") val remaining: Double? = null,
    @SerialName("resets_at") val resetsAt: String? = null,
    @SerialName("reset_interval") val resetInterval: String? = null,
) {
    fun toDomain() = ProviderBudget(
        scope,
        limit,
        spend,
        remaining,
        resetsAt?.let { Instant.parse(it).toEpochMilliseconds() },
        resetInterval,
    )
}
