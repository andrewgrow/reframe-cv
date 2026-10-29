package reframecv.ui.components.settings

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenAiConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class OpenAiProviderTest : ComponentTest() {
    @Test
    fun directAiSavesKeyWithoutRouterManagementCalls() = runBlocking<Unit> {
        val saved = CompletableDeferred<Unit>()
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultProviderEditorComponent(
                appComponentContext(),
                ProviderConnection(
                    type = ApiProviderType.OpenRouter,
                    name = "",
                    configuration = OpenRouterConfiguration(),
                ),
                saved = { saved.complete(Unit) },
                back = {},
            ).also {
                it.onType(ApiProviderType.OpenAI)
                it.onKey("  direct-key  ")
                it.onManagementKey("must-not-be-used")
                it.onRefreshAccount()
                it.onRefreshBalance()
                it.onSave()
            }
        }
        withTimeout(5000) { saved.await() }
        val connection = dependencies.providerSettingsUseCases.observe().first().single()
        assertEquals(OpenAiConfiguration("direct-key"), connection.configuration)
        runOnUiThread {
            component.onLoadModels()
            component.onType(ApiProviderType.OpenRouter)
            assertEquals(ApiProviderType.OpenAI, component.state.value.connection.type)
        }
        assertTrue(dependencies.balanceRequests.isEmpty())
    }
}
