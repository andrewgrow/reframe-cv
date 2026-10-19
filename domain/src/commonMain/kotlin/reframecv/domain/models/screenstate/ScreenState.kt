package reframecv.domain.models.screenstate

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import reframecv.domain.DomainModel

/** Identifies screens whose independent view preferences can be restored. */
enum class ScreenType { Vacancy, Resume, CoverLetter, Project, Settings }

/** Identifies a document screen or a standalone page without mixing their view states. */
data class ScreenStateOwner(
    val screen: ScreenType,
    val documentId: Long? = null,
    val projectId: Long? = null,
) {
    init {
        val global = screen == ScreenType.Settings
        require(global == (documentId == null) && global == (projectId == null))
    }
}

/** Holds arbitrary view-state fields interpreted by their owning screen. */
data class ScreenState(
    val owner: ScreenStateOwner,
    val states: JsonObject = JsonObject(emptyMap()),
) : DomainModel {
    fun text(key: String): String? =
        (states[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
