package reframecv.ui.components.common.ai

import com.arkivanov.decompose.DefaultComponentContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.screenstate.ScreenState
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.repository.ScreenStatesRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ScreenStateUseCases

class AiPromptPreferenceTest : ComponentTest() {
    private val owner = ScreenStateOwner(ScreenType.Vacancy, 12, 7)

    private suspend fun setup() {
        dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Work",
                configuration = OpenRouterConfiguration(
                    apiKey = "key",
                    modelId = "model",
                ),
            ),
        )
        dependencies.promptsUseCases.save(Prompt(text = "First"))
        dependencies.promptsUseCases.save(
            Prompt(text = "Second", component = PromptComponent.Vacancies),
        )
    }

    private suspend fun create(): DefaultAiRequestDialogComponent<Unit> {
        val dialog = runOnUiThread {
            DefaultAiRequestDialogComponent<Unit>(
                appComponentContext(),
                "resume.json + vacancy.json",
                PromptComponent.Vacancies,
                execute = { _, _ -> throw AiRequestException("Retry") },
                completed = {},
                dismissed = {},
                promptPreference = AiPromptPreference(appComponentContext(), "prompt", owner),
            )
        }
        await(dialog) { !it.loading }
        return dialog
    }

    private suspend fun await(
        dialog: AiRequestDialogComponent,
        ready: (AiRequestDialogState) -> Boolean,
    ) {
        val result = CompletableDeferred<Unit>()
        runOnUiThread { dialog.state.subscribe { if (ready(it)) result.complete(Unit) } }
        withTimeout(5000) { result.await() }
    }

    @Test
    fun remembersOnlyUsedSelectionAndRestoresCurrentSavedTextWithoutOverwritingOtherState() =
        runBlocking {
            setup()
            dependencies.screenStateUseCases.update(
                owner,
                buildJsonObject {
                    put("prompt", 1)
                    put("job_details", "collapsed")
                },
            )
            val cancelled = create()
            runOnUiThread {
                assertEquals(1L, cancelled.state.value.promptId)
                cancelled.onPrompt(2)
                cancelled.onClose()
            }
            val dialog = create()
            runOnUiThread {
                assertEquals(1L, dialog.state.value.promptId)
                dialog.onPrompt(2)
                dialog.onText("Unsaved request-local text")
                dialog.onRun()
            }
            await(dialog) { it.error != null && !it.running }
            runOnUiThread { dialog.onClose() }
            val stored = dependencies.screenStateUseCases.load(owner)!!
            assertEquals(JsonPrimitive(2L), stored.states["prompt"])
            assertEquals("collapsed", stored.text("job_details"))
            assertEquals(setOf("prompt", "job_details"), stored.states.keys)
            dependencies.promptsUseCases.save(
                dependencies.promptsRepository.records.value[1].copy(text = "Updated in Settings"),
            )
            val reopened = create()
            runOnUiThread {
                assertEquals(2L, reopened.state.value.promptId)
                assertEquals("Updated in Settings", reopened.state.value.text)
            }
        }

    @Test
    fun unavailableOrWrongScopePromptUsesNormalChoiceRules() = runBlocking {
        setup()
        val disabled = dependencies.promptsUseCases.save(Prompt(text = "Disabled", active = false))
        val wrongScope = dependencies.promptsUseCases.save(
            Prompt(text = "Resume", component = PromptComponent.Resume),
        )
        for (id in listOf(disabled, wrongScope, 999L)) {
            dependencies.screenStateUseCases.update(owner, buildJsonObject { put("prompt", id) })
            val dialog = create()
            runOnUiThread {
                assertNull(dialog.state.value.promptId)
                dialog.onClose()
            }
        }
        dependencies.promptsRepository.records.value =
            dependencies.promptsRepository.records.value.take(1)
        val fallback = create()
        runOnUiThread { assertEquals(1L, fallback.state.value.promptId) }
    }

    @Test
    fun malformedValuesAreIgnoredAndOperationsAndDocumentsStayIndependent() = runBlocking {
        val values =
            listOf(
                JsonNull,
                JsonPrimitive("2"),
                JsonPrimitive(true),
                JsonPrimitive(-1),
                JsonArray(emptyList()),
            )
        for (value in values) {
            dependencies.screenStateUseCases.update(owner, buildJsonObject { put("prompt", value) })
            assertNull(AiPromptPreference(appComponentContext(), "prompt", owner).load())
        }
        val assessment = AiPromptPreference(appComponentContext(), "assessment", owner)
        val improvement = AiPromptPreference(appComponentContext(), "improvement", owner)
        runOnUiThread {
            assessment.remember(1)
            improvement.remember(2)
        }
        assertEquals(1L, AiPromptPreference(appComponentContext(), "assessment", owner).load())
        assertEquals(2L, AiPromptPreference(appComponentContext(), "improvement", owner).load())
        assertNull(
            AiPromptPreference(
                appComponentContext(),
                "assessment",
                owner.copy(documentId = 13),
            ).load(),
        )
    }

    @Test
    fun preferenceStorageFailuresFallBackAndKeepTheUsedSelectionInMemory() = runBlocking {
        val preferences = ScreenStateUseCases(
            object : ScreenStatesRepository {
                override suspend fun get(owner: ScreenStateOwner): ScreenState? =
                    error("Disk unavailable")
                override suspend fun update(owner: ScreenStateOwner, changes: JsonObject) {
                    error("Disk unavailable")
                }
            },
            dependencies.logger,
        )
        val selection = runOnUiThread {
            AiPromptPreference(
                DefaultAppComponentContext(
                    DefaultComponentContext(lifecycle),
                    object : ApplicationDependencies by dependencies {
                        override val screenStateUseCases = preferences
                    },
                ),
                "prompt",
                owner,
            )
        }
        assertNull(selection.load())
        runOnUiThread { selection.remember(2) }
        preferences.close()
        runOnUiThread { selection.remember(3) }
        assertEquals(3L, selection.load())
        val operations = withTimeout(5000) {
            List(3) { dependencies.logger.failures.receive().operation }.toSet()
        }
        assertEquals(
            setOf("Load AI prompt preference", "Save screen state", "Save AI prompt preference"),
            operations,
        )
    }
}
