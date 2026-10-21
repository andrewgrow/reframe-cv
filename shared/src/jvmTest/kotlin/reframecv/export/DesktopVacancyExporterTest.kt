package reframecv.export

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume

class DesktopVacancyExporterTest {
    @get:Rule val folder = TemporaryFolder()
    private val clock = Clock.fixed(Instant.parse("2026-10-14T12:34:56Z"), ZoneOffset.UTC)
    private val vacancy = Vacancy(
        id = 12, projectId = 7, name = "My shortlist",
        jobTitle = "Mobile developer", company = "Example",
        description = "Build apps.\nReview code.", url = "https://example.com/job",
        keywords = listOf("Kotlin", "Compose"), responsibilities = listOf("Ship apps, APIs"),
        requirements = listOf("Kotlin"), niceToHave = listOf("CI/CD"),
        createdAt = 10, updatedAt = 20,
        resumes = listOf(
            VacancyResume(3),
        ),
        coverLetterIds = listOf(4), vacancyAttachmentId = 5,
    )

    @Test
    fun generatesDocumentBeforePickerAndSavesOriginalContentWithSuggestedFilename() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val destination = folder.newFile("chosen.json").toPath()
        val exporter = DesktopVacancyExporter(
            ExportDestinationPicker { name ->
                assertEquals("vacancy-12-20261014-1234-56.json", name)
                assertReady(temporary)
                destination.toString()
            },
            clock,
            temporary,
        )
        assertEquals(DocumentExportResult.Saved, exporter.export(vacancy))
        val exported = Files.readString(destination)
        assertTrue(exported.startsWith("{\n    \"formatVersion\": 1,"))
        assertTrue(exported.endsWith("\n"))
        assertEquals(
            Json.parseToJsonElement(VacancyExportJson.encode(vacancy)),
            Json.parseToJsonElement(exported),
        )
        val document = Json.parseToJsonElement(exported).jsonObject
        val content = document.getValue("content").jsonObject
        assertEquals("My shortlist", content.getValue("name").jsonPrimitive.content)
        assertEquals("Mobile developer", content.getValue("jobTitle").jsonPrimitive.content)
        assertEquals(setOf("formatVersion", "content"), document.keys)
        assertEquals(
            setOf(
                "name",
                "jobTitle",
                "company",
                "description",
                "url",
                "keywords",
                "responsibilities",
                "requirements",
                "niceToHave",
            ),
            content.keys,
        )
        assertEmpty(temporary)
    }

    @Test
    fun cancellingPickerDeletesGeneratedTemporaryDocument() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val exporter = DesktopVacancyExporter(
            ExportDestinationPicker {
                assertReady(temporary)
                null
            },
            clock,
            temporary,
        )
        assertEquals(DocumentExportResult.Cancelled, exporter.export(vacancy))
        assertEmpty(temporary)
    }

    @Test
    fun pickerAndCopyFailuresDeleteTemporaryDocument() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val brokenPicker = DesktopVacancyExporter(
            ExportDestinationPicker { error("Picker unavailable") },
            clock,
            temporary,
        )
        assertFailsWith<IllegalStateException> { brokenPicker.export(vacancy) }
        assertEmpty(temporary)
        val brokenDestination = DesktopVacancyExporter(
            ExportDestinationPicker {
                folder.root.toPath().resolve("missing/vacancy.json").toString()
            },
            clock,
            temporary,
        )
        assertFailsWith<java.io.IOException> { brokenDestination.export(vacancy) }
        assertEmpty(temporary)
    }

    @Test
    fun coroutineCancellationStillDeletesTemporaryDocument() = runBlocking {
        val temporary = folder.newFolder("temp").toPath()
        val entered = CompletableDeferred<Unit>()
        val exporter = DesktopVacancyExporter(
            ExportDestinationPicker {
                assertReady(temporary)
                entered.complete(Unit)
                awaitCancellation()
            },
            clock,
            temporary,
        )
        val operation = async { exporter.export(vacancy) }
        withTimeout(10_000) {
            entered.await()
            operation.cancelAndJoin()
        }
        assertEmpty(temporary)
    }

    private fun assertReady(directory: Path) {
        Files.list(directory).use { files ->
            val temporary = files.toList().single()
            assertTrue(temporary.fileName.toString().endsWith(".json"))
            assertEquals(VacancyExportJson.encode(vacancy), Files.readString(temporary))
        }
    }

    private fun assertEmpty(directory: Path) {
        Files.list(directory).use { assertEquals(0L, it.count()) }
    }
}
