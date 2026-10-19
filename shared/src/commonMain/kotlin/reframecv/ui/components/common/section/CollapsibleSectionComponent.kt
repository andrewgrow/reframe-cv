package reframecv.ui.components.common.section

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.ui.context.AppComponentContext

/** Supplies restored visibility without exposing a document's editable data. */
data class CollapsibleSectionState(val expanded: Boolean = true, val loading: Boolean = false)

/** Lets a screen toggle a named block and bind a newly saved document to its view preferences. */
interface CollapsibleSectionComponent {
    val state: Value<CollapsibleSectionState>
    fun onToggle()
    fun onBindOwner(owner: ScreenStateOwner)
}

/** Restores per-document visibility and queues changes independently of the page lifetime. */
class DefaultCollapsibleSectionComponent(
    componentContext: AppComponentContext,
    private val sectionKey: String,
    private var owner: ScreenStateOwner? = null,
) : CollapsibleSectionComponent {
    private val dependencies = componentContext.dependencies
    private val preferences = dependencies.screenStateUseCases
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(CollapsibleSectionState(loading = owner != null))

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
        owner?.let { load(it) }
    }

    override fun onToggle() {
        if (state.value.loading) return
        state.value = state.value.copy(expanded = !state.value.expanded)
        persist()
    }

    override fun onBindOwner(owner: ScreenStateOwner) {
        if (this.owner != null) return
        this.owner = owner
        persist()
    }

    private fun persist() {
        owner?.let {
            preferences.update(
                it,
                buildJsonObject {
                    put(sectionKey, if (state.value.expanded) "expanded" else "collapsed")
                },
            )
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun load(owner: ScreenStateOwner) {
        scope.launch {
            try {
                state.value =
                    CollapsibleSectionState(
                        preferences.load(owner)?.text(sectionKey) != "collapsed",
                    )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Load section view state",
                    error,
                    mapOf(
                        "screen" to owner.screen,
                        "documentId" to owner.documentId,
                        "section" to sectionKey,
                    ),
                )
                state.value = CollapsibleSectionState()
            }
        }
    }
}
