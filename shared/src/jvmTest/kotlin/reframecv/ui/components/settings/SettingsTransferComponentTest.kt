package reframecv.ui.components.settings

import com.arkivanov.decompose.DefaultComponentContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.OpenRouterConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.export.DocumentExportResult
import reframecv.settings.SettingsBackup
import reframecv.settings.SettingsBackupFiles
import reframecv.settings.SettingsBackupRepository
import reframecv.settings.SettingsImportReport
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.SettingsBackupUseCases

class SettingsTransferComponentTest : ComponentTest() {
    private var json = ""
    private var source: String? = null
    private var restored = 0
    private var closed = 0
    private var write: suspend () -> DocumentExportResult = { DocumentExportResult.Saved }
    private fun component() = runOnUiThread {
        val useCases = SettingsBackupUseCases(
            object : SettingsBackupRepository {
                override suspend fun snapshot() = SettingsBackup(
                    connections = listOf(
                        ProviderConnection(
                            type = ApiProviderType.OpenRouter,
                            name = "",
                            configuration = OpenRouterConfiguration(
                                modelId = "model",
                                apiKey = "PRIVATE_KEY",
                            ),
                        ),
                    ),
                )
                override suspend fun restore(backup: SettingsBackup): SettingsImportReport {
                    restored++
                    return SettingsImportReport(1, 2, skipped = backup.skipped)
                }
            },
            object : SettingsBackupFiles {
                override suspend fun read() = source
                override suspend fun write(json: String): DocumentExportResult {
                    this@SettingsTransferComponentTest.json = json
                    return write()
                }
            },
        )
        val context =
            DefaultAppComponentContext(
                DefaultComponentContext(lifecycle),
                object : ApplicationDependencies by dependencies {
                    override val settingsBackupUseCases = useCases
                },
            )
        DefaultSettingsTransferComponent(context) { closed++ }
    }
    private suspend fun complete(component: SettingsTransferComponent) = withTimeout(5000) {
        val ready = CompletableDeferred<Unit>()
        val subscription = runOnUiThread {
            component.state.subscribe { if (it.operation == null) ready.complete(Unit) }
        }
        try {
            ready.await()
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun keysRequireExplicitConsentAndExportStaysOnThePage() = runBlocking<Unit> {
        val component = component()
        runOnUiThread { component.onExport() }
        complete(component)
        assertFalse("PRIVATE_KEY" in json)
        runOnUiThread {
            component.onIncludeApiKeys(true)
            component.onExport()
        }
        complete(component)
        assertTrue("PRIVATE_KEY" in json)
        assertTrue(component.state.value.exported)
        assertEquals(0, closed)
        runOnUiThread { component.onBack() }
        assertEquals(1, closed)
        assertFalse(component.state.value.includeApiKeys)
    }

    @Test
    fun importReportsResultsAndErrorsWithoutPartialRestore() = runBlocking<Unit> {
        val component = component()
        source = "not a backup PRIVATE_KEY"
        runOnUiThread { component.onImport() }
        complete(component)
        assertEquals(SettingsTransferOperation.Import, component.state.value.error)
        assertEquals(0, restored)
        source = """{"format":"reframecv-settings","prompts":[{"text":""}]}"""
        runOnUiThread { component.onImport() }
        complete(component)
        assertEquals(1, restored)
        assertEquals(1, component.state.value.report?.skipped)
        source = null
        runOnUiThread { component.onImport() }
        complete(component)
        assertEquals(null, component.state.value.report)
    }

    @Test
    fun pendingOperationBlocksReentryConsentChangesAndBackAndAllowsRetry() = runBlocking<Unit> {
        val gate = CompletableDeferred<Unit>()
        write = {
            gate.await()
            DocumentExportResult.Cancelled
        }
        val component = component()
        runOnUiThread {
            component.onExport()
            component.onImport()
            component.onIncludeApiKeys(true)
            component.onBack()
        }
        assertEquals(SettingsTransferOperation.Export, component.state.value.operation)
        assertFalse(component.state.value.includeApiKeys)
        assertEquals(0, closed)
        gate.complete(Unit)
        complete(component)
        assertFalse(component.state.value.exported)
        write = { error("File failure") }
        runOnUiThread { component.onExport() }
        complete(component)
        assertEquals(SettingsTransferOperation.Export, component.state.value.error)
        write = { DocumentExportResult.Saved }
        runOnUiThread { component.onExport() }
        complete(component)
        assertTrue(component.state.value.exported)
    }
}
