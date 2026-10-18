package reframecv.domain.models.providers

import kotlinx.serialization.Serializable

/** Keeps the account's purchased credits and spending separate from API-key limits. */
@Serializable
data class ProviderCreditBalance(
    val totalCreditsUsd: Double,
    val totalUsageUsd: Double,
    val fetchedAt: Long,
) {
    val remainingUsd: Double get() = totalCreditsUsd - totalUsageUsd
}
