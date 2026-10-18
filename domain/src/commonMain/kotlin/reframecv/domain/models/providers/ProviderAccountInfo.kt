package reframecv.domain.models.providers

import kotlinx.serialization.Serializable

/** Describes one provider spending limit independently of its API representation. */
@Serializable
data class ProviderBudget(
    val scope: String,
    val limitUsd: Double? = null,
    val spendUsd: Double? = null,
    val remainingUsd: Double? = null,
    val resetsAt: Long? = null,
    val resetInterval: String? = null,
)

/** Keeps the last known provider budgets and key expiry available offline. */
@Serializable
data class ProviderAccountInfo(
    val budgets: List<ProviderBudget> = emptyList(),
    val effectiveBudget: ProviderBudget? = null,
    val expiresAt: Long? = null,
    val fetchedAt: Long,
)
