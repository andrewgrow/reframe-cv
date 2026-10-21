package reframecv.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import reframecv.domain.models.prompts.Prompt
import reframecv.domain.models.prompts.PromptComponent
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ProviderAccountInfo
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.settings.AppSettings
import reframecv.domain.models.settings.WindowSettings

class SettingsBackupJsonTest {
    private val connection = ProviderConnection(
        id = 42,
        type = ApiProviderType.OpenRouter,
        name = "Personal",
        apiKey = "SECRET_API",
        managementApiKey = "SECRET_MANAGEMENT",
        modelId = "example/model",
        active = false,
        accountInfo = ProviderAccountInfo(fetchedAt = 123),
    )
    private val backup = SettingsBackup(
        AppSettings(WindowSettings(1200f, 900f, true)),
        listOf(connection),
        listOf(
            Prompt(
                title = "Extract",
                text = "Read the document",
                component = PromptComponent.Vacancies,
                active = false,
            ),
        ),
    )

    @Test
    fun roundTripRestoresPortableFieldsAndOnlyExplicitlyIncludedCredentials() {
        val decoded = SettingsBackupJson.decode(SettingsBackupJson.encode(backup, true))
        assertEquals(backup.settings, decoded.settings)
        assertEquals(connection.copy(id = 0, accountInfo = null), decoded.connections.single())
        assertEquals(backup.prompts, decoded.prompts)
        val publicJson = SettingsBackupJson.encode(backup, false)
        listOf(
            "SECRET_API",
            "SECRET_MANAGEMENT",
            "apiKey",
            "managementApiKey",
            "accountInfo",
            "createdAt",
        )
            .forEach { assertFalse(it in publicJson, it) }
        assertEquals("", SettingsBackupJson.decode(publicJson).connections.single().apiKey)
        assertFalse("SECRET_API" in backup.toString())
    }

    @Test
    fun futureFieldsAreIgnoredAndOptionalMissingValuesReceiveDefaults() {
        val decoded = SettingsBackupJson.decode(
            """{"format":"reframecv-settings","future":true,"settings":{"width":950},
                "connections":[{"type":"OpenRouter","future":"field"}],
                "prompts":[{"text":"Instructions","future":{}}]}""",
        )
        assertEquals(WindowSettings(950f, 600f), decoded.settings?.window)
        assertEquals("", decoded.connections.single().name)
        assertTrue(decoded.connections.single().active)
        assertNull(decoded.prompts.single().component)
        assertEquals(0, decoded.skipped)
    }

    @Test
    fun invalidAndUnsupportedRecordsDoNotDiscardValidNeighbors() {
        val decoded = SettingsBackupJson.decode(
            """{"format":"reframecv-settings","settings":{"width":-1},
                "connections":[{"type":"Future"},{"type":"OpenRouter","name":42},{"type":"OpenRouter"}],
                "prompts":[{"text":""},{"text":"x","component":"Future"},null,{"text":"Keep"}]}""",
        )
        assertEquals(6, decoded.skipped)
        assertNull(decoded.settings)
        assertEquals(1, decoded.connections.size)
        assertEquals("Keep", decoded.prompts.single().text)
    }

    @Test
    fun invalidEnvelopeAndFutureVersionsFailWithoutExposingInput() {
        listOf(
            "{}",
            "SECRET_API",
            """{"format":"other"}""",
            """{"format":"reframecv-settings","schemaVersion":2}""",
            """{"format":"reframecv-settings","connections":{}}""",
        ).forEach {
            val failure =
                assertFailsWith<SettingsBackupFormatException> { SettingsBackupJson.decode(it) }
            assertNull(failure.cause)
            assertFalse("SECRET_API" in failure.toString())
        }
    }
}
