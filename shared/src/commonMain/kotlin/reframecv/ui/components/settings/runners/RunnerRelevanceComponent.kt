package reframecv.ui.components.settings.runners

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.relevance.RunnerRelevanceException
import reframecv.ui.components.common.ai.AiPromptPreference
import reframecv.ui.components.common.ai.AiRequestDialogSlot
import reframecv.ui.components.common.ai.AiRequestException
import reframecv.ui.components.common.ai.DefaultAiRequestDialogComponent
import reframecv.ui.context.AppComponentContext

/** Holds the saved score whose original criteria are being inspected. */
data class RunnerRelevanceSelection(val result: RunnerResult? = null)

/** Opens one relevance request or the criteria behind a previously saved score. */
interface RunnerRelevanceComponent {
    val dialog: Value<AiRequestDialogSlot>
    val selection: Value<RunnerRelevanceSelection>
    val isBusy: Boolean get() = dialog.value.component?.state?.value?.running == true
    fun assess(id: Long)
    fun view(id: Long)
    fun closeResult()
    fun onBack(): Boolean
}

/** Returns only validated, persisted feedback to the matching visible dataset row. */
class DefaultRunnerRelevanceComponent(
    private val context: AppComponentContext,
    private val state: MutableValue<RunnerRunsState>,
    private val pause: () -> Unit = {},
    private val resume: () -> Unit = {},
    private val canOpen: () -> Boolean,
) : RunnerRelevanceComponent {
    override val dialog = MutableValue(AiRequestDialogSlot())
    override val selection = MutableValue(RunnerRelevanceSelection())
    private val preference = AiPromptPreference(
        context,
        "runner_relevance_prompt",
        ScreenStateOwner(ScreenType.Application),
    )
    private fun row(id: Long) = state.value.view?.page?.savedResults?.find { it.id == id }
    override fun assess(id: Long) {
        if (!canOpen() || dialog.value.component != null || selection.value.result != null) return
        val source = row(id) ?: return
        context.dependencies.eventHistory.record(
            "Runner relevance opened(runId=${source.runId}, resultId=${source.id})",
        )
        pause()
        dialog.value = AiRequestDialogSlot(
            DefaultAiRequestDialogComponent(
                context,
                "${state.value.runner.name} · result #${source.id} (runner-result.json)",
                PromptComponent.Vacancies,
                execute = { connection, instruction ->
                    try {
                        context.dependencies.assessRunnerResult(
                            source.runId,
                            source.id,
                            connection.id,
                            instruction,
                        )
                    } catch (error: RunnerRelevanceException) {
                        throw AiRequestException(
                            error.message ?: "The relevance request failed.",
                            error,
                        )
                    }
                },
                completed = ::apply,
                dismissed = {
                    dialog.value = AiRequestDialogSlot()
                    resume()
                },
                promptPreference = preference,
            ),
        )
    }
    private fun apply(result: RunnerResult) {
        val view = state.value.view
        val page = view?.page
        if (view?.run?.id == result.runId && page != null) {
            state.value = state.value.copy(
                view = view.copy(
                    page = page.copy(
                        savedResults = page.savedResults.map {
                            if (it.id == result.id) {
                                it.copy(
                                    relevanceScore = result.relevanceScore,
                                    relevancePrompt = result.relevancePrompt,
                                )
                            } else {
                                it
                            }
                        },
                    ),
                ),
            )
        }
        dialog.value = AiRequestDialogSlot()
        resume()
    }
    override fun view(id: Long) {
        if (!canOpen() || dialog.value.component != null) return
        row(id)?.takeIf { it.relevanceScore != null }?.let {
            selection.value = RunnerRelevanceSelection(it)
        }
    }
    override fun closeResult() {
        selection.value = RunnerRelevanceSelection()
    }
    override fun onBack(): Boolean {
        val component = dialog.value.component
        val handled = component != null || selection.value.result != null
        component?.onClose()
        closeResult()
        return handled
    }
}
