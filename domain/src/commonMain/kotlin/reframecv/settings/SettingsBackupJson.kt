package reframecv.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ConnectionOperation
import reframecv.domain.models.providers.ConnectionRunner
import reframecv.domain.models.providers.ProviderConfigurationJson
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

/** Maintains a versioned portable format and isolates malformed or future records during import. */
object SettingsBackupJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        prettyPrint = true
    }

    fun encode(backup: SettingsBackup, includeApiKeys: Boolean): String = json.encodeToString(
        BackupDocument(
            format = "reframecv-settings",
            settings = backup.settings?.let {
                json.encodeToJsonElement(
                    WindowRecord(it.window.width, it.window.height, it.window.maximized),
                )
            },
            connections = backup.connections.map {
                json.encodeToJsonElement(
                    ConnectionRecord(
                        it.type.name,
                        it.name,
                        it.active,
                        portableConfiguration(it, includeApiKeys),
                        backup.runners.filter { runner ->
                            runner.connectionId == it.id
                        }.map { runner ->
                            RunnerRecord(
                                runner.name,
                                runner.operation.name,
                                runner.configuration,
                                runner.active,
                            )
                        },
                    ),
                )
            },
            prompts = backup.prompts.map {
                json.encodeToJsonElement(
                    PromptRecord(it.title, it.component?.name, it.text, it.active),
                )
            },
        ),
    )

    fun decode(text: String): SettingsBackup {
        val document = try {
            json.decodeFromString<BackupDocument>(text).also {
                require(it.format == "reframecv-settings" && it.schemaVersion == 2)
            }
        } catch (_: IllegalArgumentException) {
            throw SettingsBackupFormatException()
        }
        var skipped = 0
        fun <T> record(value: JsonElement, map: (JsonElement) -> T): T? = try {
            map(value)
        } catch (_: IllegalArgumentException) {
            skipped++
            null
        }
        val settings = document.settings?.let {
            record(it) { value ->
                val window = json.decodeFromJsonElement<WindowRecord>(value)
                AppSettings(WindowSettings(window.width, window.height, window.maximized))
            }
        }
        val runners = mutableListOf<ConnectionRunner>()
        val connections = document.connections.mapIndexedNotNull { index, value ->
            record(value) { element ->
                connection(element).copy(id = index + 1L)
            }?.also { connection ->
                val source = json.decodeFromJsonElement<ConnectionRecord>(value)
                source.runners.forEach { runner ->
                    record(json.encodeToJsonElement(runner)) {
                        ConnectionRunner(
                            connectionId = connection.id,
                            name = runner.name,
                            operation = ConnectionOperation.valueOf(runner.operation),
                            configuration = runner.configuration,
                            active = runner.active,
                        )
                    }?.let { runners += it }
                }
            }
        }
        val prompts = document.prompts.mapNotNull { value -> record(value, ::prompt) }
        return SettingsBackup(settings, connections, prompts, skipped, runners)
    }

    private fun portableConfiguration(
        connection: ProviderConnection,
        secrets: Boolean,
    ): JsonElement {
        val config = ProviderConfigurationJson.portable(connection.configuration, secrets)
        val fields = json.parseToJsonElement(ProviderConfigurationJson.encode(config)).jsonObject
        return if (secrets) {
            fields
        } else {
            JsonObject(
                fields.filterKeys {
                    it !in setOf("apiKey", "managementApiKey", "apiToken")
                },
            )
        }
    }

    private fun connection(value: JsonElement): ProviderConnection {
        val record = json.decodeFromJsonElement<ConnectionRecord>(value)
        return ProviderConnection(
            type = ApiProviderType.valueOf(record.type),
            name = record.name,
            active = record.active,
            configuration = ProviderConfigurationJson.decode(
                ApiProviderType.valueOf(record.type),
                record.configuration.toString(),
            ),
        )
    }

    private fun prompt(value: JsonElement): Prompt {
        val record = json.decodeFromJsonElement<PromptRecord>(value)
        require(record.text.isNotBlank())
        return Prompt(
            title = record.title,
            text = record.text,
            active = record.active,
            component = record.component?.let(PromptComponent::valueOf),
        )
    }
}

/** Identifies the portable settings contract and its independently readable records. */
@Serializable
private data class BackupDocument(
    val format: String,
    val schemaVersion: Int = 2,
    val settings: JsonElement? = null,
    val connections: List<JsonElement> = emptyList(),
    val prompts: List<JsonElement> = emptyList(),
)

/** Stores window preferences without database-specific columns. */
@Serializable
private data class WindowRecord(
    val width: Float = 800f,
    val height: Float = 600f,
    val maximized: Boolean = false,
)

/** Stores connection configuration with optional credentials. */
@Serializable
private data class ConnectionRecord(
    val type: String,
    val name: String = "",
    val active: Boolean = true,
    val configuration: JsonElement,
    val runners: List<RunnerRecord> = emptyList(),
)

/** Stores reusable instructions without local record identities. */
@Serializable
private data class PromptRecord(
    val title: String = "",
    val component: String? = null,
    val text: String,
    val active: Boolean = true,
)

/** Stores reusable operations with their owning portable connection. */
@Serializable
private data class RunnerRecord(
    val name: String,
    val operation: String,
    val configuration: String,
    val active: Boolean = true,
)
