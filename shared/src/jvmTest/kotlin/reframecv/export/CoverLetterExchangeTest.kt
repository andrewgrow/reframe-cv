package reframecv.export

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.importing.CoverLetterImportJson
import reframecv.serialization.CoverLetterContentJson

class CoverLetterExchangeTest {
    @get:Rule val folder = TemporaryFolder()
    private val letter = CoverLetter(
        12,
        7,
        "Letter — Über uns",
        10,
        20,
        "Dear team,\n\nKotlin & Compose.\nRegards,\nZoë",
        99,
        listOf("Kotlin", "Mobile"),
    )
    private val text = checkNotNull(
        javaClass.getResource("/cover-letter-content-v1.schema.json"),
    ).readText()
    private val registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
        it.schemas(mapOf("urn:reframecv:cover-letter-content:1" to text))
    }
    private val schema = registry.getSchema(
        SchemaLocation.of("urn:reframecv:cover-letter-content:1"),
    )

    @Test
    fun exportedDocumentsValidateAndRoundTripWithoutLocalIdentityOrLinks() {
        listOf(letter, letter.copy(name = "", content = "", keywords = emptyList())).forEach {
            val encoded = CoverLetterContentJson.encode(it)
            assertTrue(schema.validate(encoded, InputFormat.JSON).isEmpty())
            assertEquals(
                setOf("formatVersion", "content"),
                Json.parseToJsonElement(encoded).jsonObject.keys,
            )
            val content = Json.parseToJsonElement(encoded).jsonObject.getValue("content").jsonObject
            assertEquals(setOf("name", "text", "keywords"), content.keys)
            assertEquals(
                it.copy(id = 0, projectId = 0, createdAt = 0, updatedAt = 0, sourceResumeId = null),
                CoverLetterImportJson.decode(encoded),
            )
        }
        val meta = registry.getSchema(
            SchemaLocation.of("https://json-schema.org/draft/2020-12/schema"),
        )
        assertTrue(meta.validate(text, InputFormat.JSON).isEmpty())
    }

    @Test
    fun importSkipsUnknownAndMalformedFieldsButRejectsInvalidEnvelopes() {
        val decoded = CoverLetterImportJson.decode(
            "\uFEFF" + """{
            "formatVersion":1,"id":45,"projectId":99,"sourceResumeId":12,
            "content":{"name":"My letter","text":false,"keywords":["Kotlin",42,null],"future":{}}
        }""",
        )
        assertEquals(CoverLetter(0, 0, "My letter", 0, 0, keywords = listOf("Kotlin")), decoded)
        listOf(
            "[]",
            "{}",
            """{"formatVersion":2,"content":{}}""",
            """{"formatVersion":"1","content":{}}""",
            """{"formatVersion":1,"content":null}""",
        )
            .forEach {
                assertFailsWith<IllegalArgumentException> { CoverLetterImportJson.decode(it) }
            }
        assertEquals("", CoverLetterImportJson.decode("""{"formatVersion":1,"content":{}}""").name)
        assertTrue(
            schema.validate("""{"formatVersion":1,"content":{}}""", InputFormat.JSON).isNotEmpty(),
        )
    }

    @Test
    fun exportUsesFormattedJsonAndDateBasedNameThenCleansTemporaryFile() = runBlocking<Unit> {
        val temporary = folder.newFolder("temp").toPath()
        val destination = folder.newFile("chosen.json").toPath()
        val clock = Clock.fixed(Instant.parse("2026-10-21T12:34:56Z"), ZoneOffset.UTC)
        val exporter = DesktopCoverLetterExporter(
            ExportDestinationPicker {
                assertEquals("cover-letter-12-20261021-1234-56.json", it)
                Files.list(temporary).use { paths ->
                    assertEquals(
                        CoverLetterContentJson.encode(letter),
                        Files.readString(paths.toList().single()),
                    )
                }
                destination.toString()
            },
            clock,
            temporary,
        )
        assertEquals(DocumentExportResult.Saved, exporter.export(letter))
        assertTrue(Files.readString(destination).startsWith("{\n    \"formatVersion\": 1,"))
        assertEquals(CoverLetterContentJson.encode(letter), Files.readString(destination))
        Files.list(temporary).use { assertEquals(0L, it.count()) }
        val cancel = DesktopCoverLetterExporter(ExportDestinationPicker { null }, clock, temporary)
        assertEquals(DocumentExportResult.Cancelled, cancel.export(letter))
        val broken =
            DesktopCoverLetterExporter(
                ExportDestinationPicker {
                    error("Picker unavailable")
                },
                clock,
                temporary,
            )
        assertFailsWith<IllegalStateException> { broken.export(letter) }
        Files.list(temporary).use { assertEquals(0L, it.count()) }
    }
}
