package reframecv.database.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

class WorkspaceObservationTest : WorkspaceDatabaseTest() {
    @Test
    fun countsEmitForEachSectionAndBecomeZeroOnProjectDeletion() = runBlocking {
        val projectId = project()
        val values = Channel<List<Int>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            combine(
                database.resumeDao().observeCount(projectId),
                database.vacancyDao().observeCount(projectId),
                database.coverLetterDao().observeCount(projectId),
            ) { resumes, vacancies, letters -> listOf(resumes, vacancies, letters) }
                .distinctUntilChanged().collect { values.send(it) }
        }
        try {
            withTimeout(10_000.milliseconds) {
                assertEquals(listOf(0, 0, 0), values.receive())
                val resumeId = database.resumeDao().create(resume(projectId))
                awaitCounts(values, listOf(1, 0, 0))
                database.vacancyDao().create(vacancy(projectId))
                awaitCounts(values, listOf(1, 1, 0))
                database.coverLetterDao().create(letter(projectId))
                awaitCounts(values, listOf(1, 1, 1))
                database.resumeDao().softDelete(resumeId, 300)
                awaitCounts(values, listOf(0, 1, 1))
                database.projectDao().markSubtreeDeleted(projectId, 400)
                awaitCounts(values, listOf(0, 0, 0))
            }
        } finally {
            observer.cancel()
            observer.join()
            values.close()
        }
    }

    @Test
    fun keywordOnlyUpdatesAreObservedAsPartOfTheCompleteRecord() = runBlocking {
        val projectId = project()
        val dao = database.resumeDao()
        val id = dao.create(resume(projectId))
        val values = Channel<List<String>>(Channel.UNLIMITED)
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            dao.observeActive(projectId).collect {
                values.send(it.single().toDomainModel().keywords)
            }
        }
        try {
            withTimeout(10_000.milliseconds) {
                assertEquals(listOf("Kotlin", "Android", "Résumé"), values.receive())
                dao.replaceKeywords(id, listOf("Java", " Kotlin "))
                assertEquals(listOf("Java", "Kotlin"), values.receive())
            }
        } finally {
            observer.cancel()
            observer.join()
            values.close()
        }
    }

    private suspend fun awaitCounts(values: Channel<List<Int>>, expected: List<Int>) {
        while (values.receive() != expected) {
            // Room can invalidate all section counts after one transaction.
        }
    }
}
