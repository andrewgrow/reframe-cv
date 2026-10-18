package reframecv.ui.components.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
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
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.ProviderSettingsUseCases

class ProviderSavingTest : ComponentTest() {
    private val records = MutableStateFlow<List<ProviderConnection>>(emptyList())
    private var accountRequests = 0
    private val saved = CompletableDeferred<Unit>()
    private val repository = object : ProviderConnectionsRepository {
        override fun observe() = records
        override suspend fun activate(id: Long) = Unit
        override suspend fun save(connection: ProviderConnection): Long {
            records.value = listOf(connection.copy(id = 1))
            return 1
        }
        override suspend fun updateAccountInfo(
            id: Long,
            type: ApiProviderType,
            apiKey: String,
            info: ProviderAccountInfo,
        ) = Unit
    }

    private fun editor(
        models: ProviderModelsRepository,
        modelId: String = "",
    ): DefaultProviderEditorComponent = runOnUiThread {
        lifecycle.resume()
        val context = DefaultAppComponentContext(
            DefaultComponentContext(lifecycle),
            object : ApplicationDependencies by dependencies {
                override val providerSettingsUseCases = ProviderSettingsUseCases(
                    repository,
                    models,
                    ProviderAccountRepository { _, _ ->
                        accountRequests++
                        ProviderAccountInfo(fetchedAt = 1)
                    },
                )
            },
        )
        DefaultProviderEditorComponent(
            context,
            ProviderConnection(
                type = ApiProviderType.OpenRouter,
                name = "Offline",
                apiKey = " example-key ",
                modelId = modelId,
            ),
            saved = { saved.complete(Unit) },
            back = {},
        )
    }

    @Test
    fun savesCredentialsWithoutCheckingTheKeyOrChoosingAModel() = runBlocking {
        var modelRequests = 0
        val editor = editor(
            ProviderModelsRepository { _, _ ->
                modelRequests++
                error("No network")
            },
        )
        runOnUiThread { editor.onSave() }
        withTimeout(10_000) { saved.await() }
        assertEquals("example-key", records.value.single().apiKey)
        assertEquals("", records.value.single().modelId)
        assertEquals(0, modelRequests)
        assertEquals(0, accountRequests)
    }

    @Test
    fun loadingADifferentCatalogDoesNotClearTheConfiguredModel() = runBlocking {
        val editor = editor(
            ProviderModelsRepository { _, _ -> listOf(AiModel("new", "New model")) },
            modelId = "previous",
        )
        val loaded = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            val subscription = editor.state.subscribe {
                if (it.models.any { model -> model.id == "new" }) loaded.complete(Unit)
            }
            editor.onLoadModels()
            subscription
        }
        try {
            withTimeout(10_000) { loaded.await() }
            runOnUiThread {
                assertEquals("previous", editor.state.value.connection.modelId)
                editor.onSave()
            }
            withTimeout(10_000) { saved.await() }
            assertEquals("previous", records.value.single().modelId)
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun saveCancelsPendingVerificationAndLateResultsCannotReplaceTheSavedDraft() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val returned = CompletableDeferred<Unit>()
        val editor = editor(
            ProviderModelsRepository { _, _ ->
                started.complete(Unit)
                withContext(NonCancellable) { release.await() }
                returned.complete(Unit)
                listOf(AiModel("late", "Late model"))
            },
        )
        runOnUiThread { editor.onLoadModels() }
        withTimeout(10_000) { started.await() }
        runOnUiThread {
            assertTrue(editor.state.value.loading)
            editor.onSave()
        }
        withTimeout(10_000) { saved.await() }
        release.complete(Unit)
        withTimeout(10_000) { returned.await() }
        runOnUiThread {
            assertFalse(editor.state.value.loading)
            assertTrue(editor.state.value.models.isEmpty())
            assertEquals("", records.value.single().modelId)
            assertEquals(0, accountRequests)
        }
    }
}
