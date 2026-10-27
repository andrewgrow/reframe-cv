package reframecv.ui.components.common.ai

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.ui.context.AppComponentContext

/** Remembers the last used prompt for one screen operation without storing request-local edits. */
class AiPromptPreference(
    context: AppComponentContext,
    private val key: String,
    private var owner: ScreenStateOwner? = null,
    private val fallbackOwner: ScreenStateOwner? = null,
) {
    private val dependencies = context.dependencies
    private val preferences = dependencies.screenStateUseCases
    private var lastUsedId: Long? = null

    @Suppress("TooGenericExceptionCaught")
    suspend fun load(): Long? {
        val screen = owner ?: return lastUsedId
        return try {
            read(screen) ?: lastUsedId ?: fallbackOwner?.let { read(it) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            report("Load AI prompt preference", error)
            lastUsedId
        }
    }

    private suspend fun read(screen: ScreenStateOwner): Long? =
        (preferences.load(screen)?.states?.get(key) as? JsonPrimitive)
            ?.takeIf { !it.isString }?.longOrNull?.takeIf { it > 0 }

    fun remember(id: Long) {
        require(id > 0)
        lastUsedId = id
        persist()
    }

    fun onBindOwner(owner: ScreenStateOwner) {
        if (this.owner != null) return
        this.owner = owner
        persist()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun persist() {
        val screen = owner ?: return
        val id = lastUsedId ?: return
        try {
            preferences.update(screen, buildJsonObject { put(key, id) })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            report("Save AI prompt preference", error)
        }
    }

    private fun report(operation: String, error: Exception) {
        dependencies.logger.error(
            operation,
            error,
            mapOf("screen" to owner?.screen, "documentId" to owner?.documentId, "field" to key),
        )
    }
}
