package reframecv.ui.components.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.providers.AiModel
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException
import reframecv.testing.ComponentTest
import reframecv.ui.components.application.DefaultRootComponent
import reframecv.ui.components.application.RootComponent
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProviderSettingsUseCases

class SettingsComponentTest : ComponentTest() {
    private val records = MutableStateFlow<List<ProviderConnection>>(emptyList())
    private val repository = object : ProviderConnectionsRepository {
        override fun observe() = records
        override suspend fun save(connection: ProviderConnection): Long {
            records.value = listOf(connection.copy(id = 1))
            return 1
        }
        override suspend fun activate(id: Long) = Unit
        override suspend fun updateAccountInfo(
            id: Long,
            type: ApiProviderType,
            apiKey: String,
            info: ProviderAccountInfo,
        ) = Unit
    }
    private fun connection() =
        ProviderConnection(type = ApiProviderType.OpenRouter, name = "", apiKey = "", modelId = "")
    private fun context(models: ProviderModelsRepository) = DefaultAppComponentContext(
        DefaultComponentContext(lifecycle),
        object : ApplicationDependencies by dependencies {
            override val providerSettingsUseCases = ProviderSettingsUseCases(
                repository,
                models,
                ProviderAccountRepository { _, _ ->
                    ProviderAccountInfo(fetchedAt = reframecv.shared.nowMillis())
                },
            )
        },
    )

    @Test
    fun settingsAndItsEditorParticipateInBackNavigation() = runOnUiThread<Unit> {
        lifecycle.resume()
        val root = DefaultRootComponent(appComponentContext())
        root.onSettings()
        val page = assertIs<RootComponent.Child.Settings>(
            root.childStack.value.active.instance,
        ).component
        page.onAdd()
        assertTrue(page.editorSlot.value.child != null)
        (root.backHandler as BackDispatcher).back()
        assertEquals(null, page.editorSlot.value.child)
        assertIs<RootComponent.Child.Settings>(root.childStack.value.active.instance)
        page.onBack()
        assertIs<RootComponent.Child.Projects>(root.childStack.value.active.instance)
    }

    @Test
    fun loadsAndSelectsModelsOnlyWhenRequested() = runBlocking {
        var saved = false
        val editor = runOnUiThread {
            lifecycle.resume()
            DefaultProviderEditorComponent(
                context(
                    ProviderModelsRepository { _, key ->
                        assertEquals("example-key", key)
                        listOf(AiModel("model-a", "Model A", true))
                    },
                ),
                connection(),
                saved = { saved = true },
                back = {},
            )
        }
        runOnUiThread {
            editor.onSave()
            assertEquals(SettingsError.Validation, editor.state.value.error)
            editor.onName(" Work ")
            editor.onKey(" example-key ")
            editor.onLoadModels()
        }
        await(editor.state) { !it.loading && it.models.isNotEmpty() }
        assertTrue(records.value.isEmpty())
        runOnUiThread {
            assertTrue(editor.state.value.valid)
            editor.onModel("unknown")
            assertEquals("", editor.state.value.connection.modelId)
            editor.onModel("model-a")
            editor.onSave()
        }
        withTimeout(10_000) { records.firstNonEmpty() }
        runOnUiThread { assertTrue(saved) }
        assertEquals("Work", records.value.single().name)
        assertEquals("example-key", records.value.single().apiKey)
        assertFalse(records.value.single().toString().contains("example-key"))
    }

    @Test
    fun failedKeyChecksKeepTheSelectedModelAndDoNotPreventSaving() = runBlocking {
        val editor = runOnUiThread {
            lifecycle.resume()
            DefaultProviderEditorComponent(
                context(
                    ProviderModelsRepository { _, _ ->
                        throw ProviderSetupException(ProviderSetupError.Unauthorized)
                    },
                ),
                connection().copy(
                    id = 7,
                    name = "Keep name",
                    apiKey = "old",
                    modelId = "old-model",
                ),
                {
                },
                {},
            )
        }
        runOnUiThread {
            editor.onKey("new-key")
            assertTrue(editor.state.value.models.isEmpty())
            assertEquals("old-model", editor.state.value.connection.modelId)
            editor.onLoadModels()
        }
        await(editor.state) { it.error == SettingsError.Unauthorized }
        runOnUiThread {
            assertEquals("Keep name", editor.state.value.connection.name)
            assertEquals("new-key", editor.state.value.connection.apiKey)
            assertTrue(editor.state.value.valid)
            assertTrue(records.value.isEmpty())
            editor.onSave()
        }
        withTimeout(10_000) { records.firstNonEmpty() }
        assertEquals("new-key", records.value.single().apiKey)
        assertEquals("old-model", records.value.single().modelId)
    }

    @Test
    fun backCancelsLoadingAndLateModelsCannotChangeTheDraft() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        var closed = false
        val editor = runOnUiThread {
            lifecycle.resume()
            DefaultProviderEditorComponent(
                context(
                    ProviderModelsRepository { _, _ ->
                        started.complete(Unit)
                        withContext(NonCancellable) { release.await() }
                        returned.complete(Unit)
                        listOf(AiModel("late", "Late"))
                    },
                ),
                connection().copy(apiKey = "key"),
                {},
                { closed = true },
            )
        }
        runOnUiThread { editor.onLoadModels() }
        withTimeout(10_000) { started.await() }
        runOnUiThread {
            editor.onBack()
            assertFalse(closed)
        }
        release.complete(Unit)
        withTimeout(10_000) { returned.await() }
        runOnUiThread {
            assertFalse(editor.state.value.loading)
            assertTrue(editor.state.value.models.isEmpty())
            editor.onBack()
            assertTrue(closed)
        }
    }

    private suspend fun <T : Any> await(value: Value<T>, predicate: (T) -> Boolean) {
        val ready = CompletableDeferred<Unit>()
        val subscription =
            runOnUiThread { value.subscribe { if (predicate(it)) ready.complete(Unit) } }
        try {
            withTimeout(10_000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }
    private suspend fun MutableStateFlow<List<ProviderConnection>>.firstNonEmpty() {
        first { it.isNotEmpty() }
    }
}
