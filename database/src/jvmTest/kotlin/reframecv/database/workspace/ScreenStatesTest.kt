package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.repository.LocalScreenStatesRepository

class ScreenStatesTest : WorkspaceDatabaseTest() {
    private fun change(key: String, value: String) = buildJsonObject { put(key, value) }

    @Test
    fun restoresIndependentOwnersAndMergesFieldsWithoutChangingDocuments() = runBlocking {
        val project = project()
        val first = database.vacancyDao().create(vacancy(project))
        val second = database.vacancyDao().create(vacancy(project))
        val resume = database.resumeDao().create(resume(project))
        val before = database.vacancyDao().findById(first)?.toDomainModel()
        var repository = LocalScreenStatesRepository(database.screenStatesDao())
        val owners = listOf(
            owner(ScreenType.Vacancy, first, project),
            owner(ScreenType.Vacancy, second, project),
            owner(ScreenType.Resume, resume, project),
        )
        assertNull(repository.get(owners.first()))
        owners.forEachIndexed { index, owner ->
            repository.update(owner, change("details", if (index == 1) "expanded" else "collapsed"))
        }
        repository.update(owners.first(), change("selected_tab", "attachments"))
        closeDatabase()
        openDatabase()
        repository = LocalScreenStatesRepository(database.screenStatesDao())
        assertEquals("collapsed", repository.get(owners[0])?.text("details"))
        assertEquals("expanded", repository.get(owners[1])?.text("details"))
        assertEquals("collapsed", repository.get(owners[2])?.text("details"))
        assertEquals("attachments", repository.get(owners[0])?.text("selected_tab"))
        assertEquals(before, database.vacancyDao().findById(first)?.toDomainModel())
    }

    @Test
    fun removesDocumentPreferencesAndIgnoresLateWritesAfterDeletion() = runBlocking {
        val project = project()
        val vacancy = database.vacancyDao().create(vacancy(project))
        val resume = database.resumeDao().create(resume(project))
        val letter = database.coverLetterDao().create(letter(project))
        val repository = LocalScreenStatesRepository(database.screenStatesDao())
        val owners = listOf(
            owner(ScreenType.Vacancy, vacancy, project),
            owner(ScreenType.Resume, resume, project),
            owner(ScreenType.CoverLetter, letter, project),
        )
        owners.forEach { repository.update(it, change("details", "collapsed")) }
        database.vacancyDao().delete(vacancy, 300)
        database.resumeDao().delete(resume, 300)
        database.coverLetterDao().delete(letter, 300)
        owners.forEach {
            repository.update(it, change("details", "expanded"))
            assertNull(repository.get(it))
        }
    }

    @Test
    fun projectDeletionCleansDocumentStateAndGlobalScreenPreferencesRemain() = runBlocking {
        val project = project()
        val other = project("Other")
        val vacancy = database.vacancyDao().create(vacancy(project))
        val repository = LocalScreenStatesRepository(database.screenStatesDao())
        val scoped = listOf(
            owner(ScreenType.Vacancy, vacancy, project),
            owner(ScreenType.Project, project, project),
        )
        val global = ScreenStateOwner(ScreenType.Settings)
        scoped.forEach { repository.update(it, change("details", "collapsed")) }
        repository.update(global, change("details", "collapsed"))
        val invalid = owner(ScreenType.Vacancy, vacancy, other)
        repository.update(invalid, change("invalid", "collapsed"))
        assertNull(repository.get(invalid))
        database.projectDao().deleteSubtree(project)
        scoped.forEach { assertNull(repository.get(it)) }
        assertEquals("collapsed", repository.get(global)?.text("details"))
    }

    @Test
    fun applicationPanelStateSurvivesDatabaseReopeningAndProjectDeletion() = runBlocking {
        val project = project()
        val application = ScreenStateOwner(ScreenType.Application)
        val settings = ScreenStateOwner(ScreenType.Settings)
        var repository = LocalScreenStatesRepository(database.screenStatesDao())
        repository.update(application, change("navigation_panel", "collapsed"))
        repository.update(settings, change("tab", "providers"))
        database.projectDao().deleteSubtree(project)
        closeDatabase()
        openDatabase()
        repository = LocalScreenStatesRepository(database.screenStatesDao())
        assertEquals("collapsed", repository.get(application)?.text("navigation_panel"))
        assertEquals("providers", repository.get(settings)?.text("tab"))
    }

    private fun owner(screen: ScreenType, id: Long, project: Long) =
        ScreenStateOwner(screen, id, project)
}
