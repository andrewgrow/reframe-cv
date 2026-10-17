package reframecv.importing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import reframecv.domain.models.vacancy.Vacancy
import reframecv.export.VacancyExportJson
import reframecv.ui.components.projects.vacancies.editor.VacancyDraft

class VacancyImportJsonTest {
    @Test
    fun exportedVacancyRoundTripsAllPortableFieldsThroughTheDraft() {
        val original = Vacancy(
            id = 17, projectId = 3, name = "My shortlist",
            jobTitle = "Développeur Kotlin", createdAt = 100, updatedAt = 200,
            description = "Build apps.\nWrite tests.",
            company = "Example", url = "https://example.com/job",
            keywords = listOf(
                "Kotlin",
                "Compose",
            ),
            responsibilities = listOf("Build, ship", "Mentor"),
            requirements = listOf("Experience\nwith Android"), niceToHave = listOf("Deutsch"),
            resumeId = 4, coverLetterId = 5, vacancyAttachmentId = 6,
        )
        val imported = VacancyImportJson.decode(VacancyExportJson.encode(original))
        val expected = original.copy(
            id = 0,
            projectId = 0,
            createdAt = 0,
            updatedAt = 0,
            resumeId = null,
            coverLetterId = null,
            vacancyAttachmentId = null,
        )
        assertEquals(expected, imported)
        val draft = VacancyDraft.fromVacancy(imported)
        assertEquals("Kotlin, Compose", draft.keywords)
        assertEquals(expected.copy(projectId = 9), draft.toVacancy(9))
    }

    @Test
    fun ignoresUnknownFieldsAndInvalidValuesWithoutImportingLocalIdentity() {
        val imported = VacancyImportJson.decode(
            """{"formatVersion":1,"unknown":true,"content":{
            "name":"My list","jobTitle":"Developer","company":42,"description":null,"url":false,
            "keywords":["Kotlin",42,null,"Compose"],"requirements":"invalid",
            "responsibilities":[{},"Build, ship"],"niceToHave":["German"],
            "id":17,"projectId":9,"vacancyAttachmentId":8,"filePath":"/private/file",
            "future":{"field":"unknown"}
        }}""",
        )
        assertEquals(
            Vacancy(
                projectId = 0,
                name = "My list",
                jobTitle = "Developer",
                createdAt = 0,
                updatedAt = 0,
                keywords = listOf("Kotlin", "Compose"),
                responsibilities = listOf("Build, ship"),
                niceToHave = listOf("German"),
            ),
            imported,
        )
        assertEquals("", VacancyImportJson.decode("""{"formatVersion":1,"content":{}}""").name)
    }

    @Test
    fun importsLegacyTitleAsNameWithoutOverridingAnExplicitName() {
        val legacy = VacancyImportJson.decode(
            """{"formatVersion":1,"content":{"title":"Old name"}}""",
        )
        assertEquals("Old name", legacy.name)
        assertEquals("", legacy.jobTitle)
        val current = VacancyImportJson.decode(
            """{"formatVersion":1,"content":{
            "title":"Legacy","name":"My name","jobTitle":"Backend developer"
        }}""",
        )
        assertEquals("My name", current.name)
        assertEquals("Backend developer", current.jobTitle)
    }

    @Test
    fun rejectsBrokenDocumentsAndUnsupportedVersions() {
        assertFailsWith<SerializationException> { VacancyImportJson.decode("invalid JSON") }
        listOf(
            "[]",
            "null",
            "{}",
            """{"formatVersion":2,"content":{}}""",
            """{"formatVersion":"1","content":{}}""",
            """{"formatVersion":1,"content":[]}""",
        ).forEach { value ->
            assertFailsWith<IllegalArgumentException> { VacancyImportJson.decode(value) }
        }
    }
}
