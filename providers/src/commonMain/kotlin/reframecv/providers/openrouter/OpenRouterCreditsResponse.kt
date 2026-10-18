package reframecv.providers.openrouter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import reframecv.domain.models.providers.ProviderCreditBalance
import reframecv.shared.nowMillis

/** Reads purchased and used account credits from the management-only endpoint. */
@Serializable
internal data class OpenRouterCreditsResponse(val data: OpenRouterCreditsData)

/** Converts credit totals without treating a key's spending limit as an account balance. */
@Serializable
internal data class OpenRouterCreditsData(
    @SerialName("total_credits") val totalCredits: Double,
    @SerialName("total_usage") val totalUsage: Double,
) {
    fun toDomain(): ProviderCreditBalance {
        require(totalCredits.isFinite() && totalCredits >= 0)
        require(totalUsage.isFinite() && totalUsage >= 0)
        return ProviderCreditBalance(totalCredits, totalUsage, nowMillis())
    }
}
