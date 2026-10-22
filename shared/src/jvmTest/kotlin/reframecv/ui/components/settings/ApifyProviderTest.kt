package reframecv.ui.components.settings

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class ApifyProviderTest : ComponentTest() {
    @Test
    fun apifyTokenSavesWithoutAiSetupAndSavedConnectionCannotChangeType() = runBlocking<Unit> {
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
                it.onType(ApiProviderType.Apify)
                it.onKey("  token  ")
                it.onLoadModels()
                it.onRefreshAccount()
                it.onRefreshBalance()
                it.onSave()
            }
        }
        withTimeout(5000) { saved.await() }
        val connection = dependencies.providerSettingsUseCases.observe().first().single()
        assertEquals("token", assertIs<ApifyConfiguration>(connection.configuration).apiToken)
        runOnUiThread {
            component.onType(ApiProviderType.OpenRouter)
            assertEquals(ApiProviderType.Apify, component.state.value.connection.type)
            assertTrue(component.state.value.successfulSaves > 0)
        }
    }
}
