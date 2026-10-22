package reframecv.ui.components.settings

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.repository.ProviderAccountRepository
import reframecv.repository.ProviderConnectionsRepository
import reframecv.repository.ProviderModelsRepository
import reframecv.repository.ProviderSetupError
import reframecv.repository.ProviderSetupException
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
        override suspend fun get(id: Long) = records.value.find { it.id == id }
        override suspend fun setActive(id: Long, active: Boolean) {
            records.value = records.value.map { if (it.id == id) it.copy(active = active) else it }
        }
        override suspend fun save(connection: ProviderConnection): Long {
            val id =
                connection.id.takeIf { it != 0L }
                    ?: ((records.value.maxOfOrNull { it.id } ?: 0L) + 1)
            records.value = records.value.filterNot { it.id == id } + connection.copy(id = id)
            return id
        }
        override suspend fun updateCreditBalance(
            id: Long,
            type: ApiProviderType,
            managementApiKey: String,
            balance: reframecv.domain.models.providers.ProviderCreditBalance,
        ) = Unit
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
                configuration = OpenRouterConfiguration(
                    apiKey = " example-key ",
                    modelId = modelId,
                ),
            ),
            saved = { saved.complete(Unit) },
            back = {},
        )
    }

    @Test
    fun modelDiscoveryShowsProviderMessageAndDoesNotReuseItForUnrelatedFailures() = runBlocking {
        var expired = true
        val editor = editor(
            ProviderModelsRepository { _, _ ->
                if (expired) {
                    throw ProviderSetupException(
                        ProviderSetupError.Unauthorized,
                        "API key expired.",
                    )
                }
                error("Unsafe transport details with credentials")
            },
        )
        suspend fun load() {
            val failed = CompletableDeferred<Unit>()
            runOnUiThread {
                editor.onLoadModels()
                editor.state.subscribe { if (it.error != null) failed.complete(Unit) }
            }
            withTimeout(5000) { failed.await() }
        }
        load()
        runOnUiThread {
            assertEquals(SettingsError.Unauthorized, editor.state.value.error)
            assertEquals("API key expired.", editor.state.value.errorDetails)
            expired = false
        }
        load()
        runOnUiThread {
            assertEquals(SettingsError.Unavailable, editor.state.value.error)
            assertNull(editor.state.value.errorDetails)
        }
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
        runOnUiThread {
            editor.onManagementKey(" management-key ")
            editor.onSave()
        }
        withTimeout(10_000) { saved.await() }
        assertEquals("example-key", records.value.single().openRouter.apiKey)
        assertEquals("", records.value.single().openRouter.modelId)
        assertEquals(0, modelRequests)
        assertEquals(0, accountRequests)
        assertEquals("management-key", editor.state.value.connection.openRouter.managementApiKey)
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
                assertEquals("previous", editor.state.value.connection.openRouter.modelId)
                editor.onSave()
            }
            withTimeout(10_000) { saved.await() }
            assertEquals("previous", records.value.single().openRouter.modelId)
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun unnamedConnectionsStayEditableAndRepeatedSavesUpdateTheSameRecord() = runBlocking {
        val editor =
            editor(ProviderModelsRepository { _, _ -> error("Unexpected network request") })
        runOnUiThread {
            editor.onName("  ")
            editor.onSave()
        }
        withTimeout(10_000) { saved.await() }
        val secondSave = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            assertFalse(editor.state.value.saving)
            assertEquals(1L, editor.state.value.connection.id)
            assertEquals("", editor.state.value.connection.name)
            val sub = editor.state.subscribe {
                if (it.successfulSaves ==
                    2L
                ) {
                    secondSave.complete(Unit)
                }
            }
            editor.onName("Named later")
            editor.onSave()
            sub
        }
        try {
            withTimeout(10_000) { secondSave.await() }
            assertEquals(1, records.value.size)
            assertEquals(1L, records.value.single().id)
            assertEquals("Named later", records.value.single().name)
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun switchingOffPersistsImmediatelyAndPreventsManualRequests() = runBlocking {
        var requests = 0
        val editor = editor(
            ProviderModelsRepository { _, _ ->
                requests++
                emptyList()
            },
        )
        runOnUiThread { editor.onSave() }
        withTimeout(10_000) { saved.await() }
        runOnUiThread { editor.onActiveChange(false) }
        withTimeout(10_000) { records.first { it.singleOrNull()?.active == false } }
        runOnUiThread {
            assertFalse(editor.state.value.connection.active)
            editor.onLoadModels()
            editor.onRefreshAccount()
            assertEquals(0, requests)
            assertEquals(0, accountRequests)
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
            assertEquals("", records.value.single().openRouter.modelId)
            assertEquals(0, accountRequests)
        }
    }
}
