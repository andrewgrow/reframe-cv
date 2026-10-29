package reframecv.ui.components.common.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.OpenAiConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class AiConnectionPreferenceTest : ComponentTest() {
    private val owner = ScreenStateOwner(ScreenType.Application)
    private val routed = ProviderConnection(
        type = ApiProviderType.OpenRouter,
        name = "Router",
        configuration = OpenRouterConfiguration("router-key", "router-model"),
    )

    private suspend fun setup(): Pair<Long, Long> {
        val first = dependencies.providerSettingsUseCases.save(routed)
        val second = dependencies.providerSettingsUseCases.save(
            routed.copy(
                type = ApiProviderType.OpenAI,
                name = "Direct",
                configuration = OpenAiConfiguration("direct-key", "direct-model"),
            ),
        )
        return first to second
    }

    private suspend fun create(
        component: PromptComponent = PromptComponent.Vacancies,
    ): DefaultAiRequestDialogComponent<Unit> {
        val dialog = runOnUiThread {
            DefaultAiRequestDialogComponent<Unit>(
                appComponentContext(),
                "source.json",
                component,
                execute = { _, _ -> error("No request expected") },
                completed = {},
                dismissed = {},
            )
        }
        val ready = CompletableDeferred<Unit>()
        runOnUiThread { dialog.state.subscribe { if (!it.loading) ready.complete(Unit) } }
        withTimeout(5000) { ready.await() }
        return dialog
    }

    @Test
    fun selectionSurvivesCancellationAndIsSharedAcrossDialogsWithoutOverwritingOtherState() =
        runBlocking {
            val (first, second) = setup()
            dependencies.screenStateUseCases.update(
                owner,
                buildJsonObject {
                    put("sidebar", "collapsed")
                    put("assessment_prompt", 7)
                },
            )
            val initial = create()
            runOnUiThread {
                assertNull(initial.state.value.connectionId)
                initial.onConnection(second)
                initial.onClose()
            }
            val restored = create(PromptComponent.Resume)
            runOnUiThread {
                assertEquals(second, restored.state.value.connectionId)
                assertEquals("direct-model", restored.state.value.connection?.ai?.modelId)
                restored.onConnection(first)
                restored.onConnection(999)
                restored.onClose()
            }
            val reopened = create(PromptComponent.CoverLetters)
            runOnUiThread { assertEquals(first, reopened.state.value.connectionId) }
            val states = dependencies.screenStateUseCases.load(owner)!!
            assertEquals(JsonPrimitive(first), states.states["ai_connection_id"])
            assertEquals("collapsed", states.text("sidebar"))
            assertEquals(JsonPrimitive(7), states.states["assessment_prompt"])
        }

    @Test
    fun disabledDeletedAndNonAiConnectionsNeverRestoreAndSoleAvailableConnectionIsSelected() =
        runBlocking {
            val (first, second) = setup()
            dependencies.providerSettingsUseCases.setActive(second, false)
            val scraper = dependencies.providerSettingsUseCases.save(
                routed.copy(
                    type = ApiProviderType.Apify,
                    configuration = ApifyConfiguration("token"),
                ),
            )
            dependencies.providerSettingsUseCases.save(routed.copy(name = "Another"))
            for (id in listOf(second, scraper, 999L)) {
                dependencies.screenStateUseCases.update(
                    owner,
                    buildJsonObject {
                        put("ai_connection_id", id)
                    },
                )
                val dialog = create()
                runOnUiThread {
                    assertNull(dialog.state.value.connectionId)
                    dialog.onClose()
                }
            }
            dependencies.providerSettingsUseCases.setActive(first, false)
            val fallback = create()
            runOnUiThread {
                assertEquals("Another", fallback.state.value.connection?.name)
                fallback.onClose()
            }
        }

    @Test
    fun malformedPreferencesFallBackToNormalSelection() = runBlocking {
        setup()
        for (value in listOf(
            JsonNull,
            JsonPrimitive("2"),
            JsonPrimitive(true),
            JsonPrimitive(-1),
            JsonPrimitive(0),
            JsonArray(emptyList()),
        )) {
            dependencies.screenStateUseCases.update(
                owner,
                buildJsonObject {
                    put("ai_connection_id", value)
                },
            )
            val dialog = create()
            runOnUiThread {
                assertNull(dialog.state.value.connectionId)
                dialog.onClose()
            }
        }
    }
}
