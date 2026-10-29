package reframecv.ui.components.common.ai

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.ui.context.AppComponentContext

/** Shares the last selected AI connection across documents and operations, including restarts. */
internal class AiConnectionPreference(context: AppComponentContext) {
    private val dependencies = context.dependencies
    private val owner = ScreenStateOwner(ScreenType.Application)

    @Suppress("TooGenericExceptionCaught")
    suspend fun load(): Long? = try {
        (dependencies.screenStateUseCases.load(owner)?.states?.get(KEY) as? JsonPrimitive)
            ?.takeIf { !it.isString }?.longOrNull?.takeIf { it > 0 }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        report("Load AI connection preference", error)
        null
    }

    @Suppress("TooGenericExceptionCaught")
    fun remember(id: Long) {
        require(id > 0)
        try {
            dependencies.screenStateUseCases.update(owner, buildJsonObject { put(KEY, id) })
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            report("Save AI connection preference", error)
        }
    }

    private fun report(operation: String, error: Exception) {
        dependencies.logger.error(operation, error, mapOf("screen" to owner.screen, "field" to KEY))
    }

    companion object {
        private const val KEY = "ai_connection_id"
    }
}
