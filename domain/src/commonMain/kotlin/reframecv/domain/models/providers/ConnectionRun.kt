package reframecv.domain.models.providers

import reframecv.domain.DomainModel

/** Tracks a saved operation's execution independently of later configuration edits. */
data class ConnectionRun(
    val id: Long = 0,
    val runnerId: Long,
    val configurationSnapshot: String,
    val state: ConnectionRunState = ConnectionRunState.Pending,
    val externalId: String? = null,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val error: String? = null,
) : DomainModel {
    override fun toString() = "ConnectionRun(id=$id, runnerId=$runnerId, state=$state)"
}

/** Describes progress that can be restored after reopening the application. */
enum class ConnectionRunState { Pending, Running, Succeeded, Failed, Aborted }
