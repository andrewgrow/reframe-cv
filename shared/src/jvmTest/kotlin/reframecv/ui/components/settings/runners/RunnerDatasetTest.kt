package reframecv.ui.components.settings.runners

import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResultFilter
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.runners.RunnerResultsPage
import reframecv.testing.ComponentTest
import reframecv.ui.threading.runOnUiThread

class RunnerDatasetTest : ComponentTest() {
    private suspend fun runner(): Long {
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example",
                configuration = ApifyConfiguration("token"),
            ),
        )
        return dependencies.connectionRunnersUseCases.save(
            RunnerDraft(searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin")
                .applyTo(RunnerDraft.newRunner(owner)),
        )
    }
    private fun response(offset: Int, count: Int, next: Int?, extra: Boolean = false) =
        RunnerResultsPage(
            emptyList(),
            offset,
            next,
            75,
            rawResults = (offset until offset + count).map {
                val json = kotlinx.serialization.json.buildJsonObject {
                    put("id", kotlinx.serialization.json.JsonPrimitive("$it"))
                    put("title", kotlinx.serialization.json.JsonPrimitive("Example role"))
                    if (extra) put("extra", kotlinx.serialization.json.JsonPrimitive(true))
                }
                RunnerRawResult(it, "LinkedIn", "$it", json.toString())
            },
        )

    @Test
    fun localPagesAreOfflineAndRemoteCursorAndUnionColumnsSurviveReopening() = runBlocking<Unit> {
        val id = runner()
        val remote = dependencies.runnerExecutionRepository
        val useCases = dependencies.runnerExecutionUseCases
        val run = useCases.start(id)
        remote.page = response(0, 50, 50)
        val first = useCases.inspect(id, run.id)
        assertEquals(25, first.page!!.savedResults.size)
        assertEquals(50, first.page!!.savedCount)
        assertEquals(25, first.page!!.nextOffset)
        val connection = dependencies.providerSettingsUseCases.observe().first().single()
        dependencies.providerSettingsUseCases.setActive(connection.id, false)
        val second = useCases.dataset.page(id, run.id, 25)
        assertEquals(25, second.page!!.savedResults.first().position)
        assertEquals(1, remote.resultReads)
        assertFailsWith<IllegalArgumentException> { useCases.dataset.loadMore(id, run.id) }
        dependencies.providerSettingsUseCases.setActive(connection.id, true)
        remote.resultError = IllegalStateException("Temporary failure")
        assertFailsWith<IllegalStateException> { useCases.dataset.loadMore(id, run.id) }
        assertEquals(50, dependencies.connectionRunnersRepository.getRun(run.id)?.nextResultOffset)
        assertEquals(50, dependencies.runnerResultsRepository.count(run.id))
        remote.resultError = null
        remote.page = response(50, 25, null, true)
        val loaded = useCases.dataset.loadMore(id, run.id, 25)
        assertEquals(50, remote.requestedOffset)
        assertEquals(75, loaded.run.nextResultOffset)
        assertTrue(loaded.run.resultsComplete)
        assertTrue("extra" in loaded.run.resultColumns)
        assertEquals(75, loaded.page!!.savedCount)
        useCases.dataset.loadMore(id, run.id)
        assertEquals(3, remote.resultReads)
        assertEquals(75, dependencies.connectionRunnersRepository.getRun(run.id)?.nextResultOffset)
    }

    @Test
    fun reviewAndImportUseStoredRowsAndKeepOwnershipChecks() = runBlocking<Unit> {
        val id = runner()
        val useCases = dependencies.runnerExecutionUseCases
        val run = useCases.start(id)
        dependencies.runnerExecutionRepository.page = response(0, 1, null)
        val record = useCases.inspect(id, run.id).page!!.savedResults.single()
        val review = dependencies.runnerResultReviewUseCases
        review.review(run.id, record.id, true)
        assertEquals(
            RunnerResultStatus.Dismissed,
            dependencies.runnerResultsRepository.get(record.id)?.status,
        )
        assertFailsWith<IllegalArgumentException> { review.review(run.id + 1, record.id, false) }
        review.review(run.id, record.id, false)
        val project = dependencies.projectsRepository.createProject("Destination")
        val vacancy = review.import(run.id, record.id, project.id)
        assertEquals(vacancy, dependencies.runnerResultsRepository.get(record.id)?.vacancyId)
        assertEquals(
            RunnerResultStatus.Imported,
            dependencies.runnerResultsRepository.get(record.id)?.status,
        )
        assertFailsWith<IllegalArgumentException> { review.import(run.id, record.id, project.id) }
        assertFailsWith<IllegalArgumentException> { review.review(run.id, record.id, true) }
        assertNotNull(
            dependencies.vacanciesRepository.observeVacancies(project.id).first().single(),
        )
        assertNull(dependencies.runnerResultsRepository.get(record.id)?.previousResultId)
    }
    private suspend fun await(
        component: RunnerRunsComponent,
        check: (RunnerRunsState) -> Boolean,
    ): RunnerRunsState {
        val ready = CompletableDeferred<RunnerRunsState>()
        val subscription =
            runOnUiThread { component.state.subscribe { if (check(it)) ready.complete(it) } }
        return try {
            withTimeout(5000) { ready.await() }
        } finally {
            runOnUiThread { subscription.cancel() }
        }
    }

    @Test
    fun componentDismissesRestoresAndImportsIntoTheSelectedProject() = runBlocking<Unit> {
        val id = runner()
        val run = dependencies.runnerExecutionUseCases.start(id)
        dependencies.runnerExecutionRepository.page = response(0, 1, null)
        val project = dependencies.projectsRepository.createProject("Destination")
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultRunnerRunsComponent(
                appComponentContext(),
                dependencies.connectionRunnersRepository.records.value.single(),
                {},
            )
        }
        await(component) { !it.loadingHistory }
        runOnUiThread { component.onSelect(run.id) }
        val ready = await(component) { it.view?.page?.savedResults?.size == 1 }
        val row = ready.view!!.page!!.savedResults.single().id
        runOnUiThread { component.onResultAction(row, RunnerResultAction.Dismiss) }
        await(component) {
            !it.working &&
                it.view?.page?.savedResults?.single()?.status == RunnerResultStatus.Dismissed
        }
        runOnUiThread { component.onResultAction(row, RunnerResultAction.Restore) }
        await(component) {
            !it.working &&
                it.view?.page?.savedResults?.single()?.reviewStatus == RunnerResultStatus.New
        }
        runOnUiThread { component.onResultAction(row, RunnerResultAction.Convert) }
        val choosing = await(component) { !it.working && it.importResultId == row }
        assertTrue(choosing.projects.any { it.id == project.id })
        runOnUiThread { component.onProject(project.id) }
        val imported = await(component) {
            !it.working &&
                it.view?.page?.savedResults?.single()?.status == RunnerResultStatus.Imported
        }
        assertNull(imported.importResultId)
        assertNotNull(imported.notice)
    }

    @Test
    fun filteredPagesStayOfflineAndDownloadCursorIgnoresHiddenRows() = runBlocking<Unit> {
        val id = runner()
        val useCases = dependencies.runnerExecutionUseCases
        val remote = dependencies.runnerExecutionRepository
        val run = useCases.start(id)
        remote.page = response(0, 50, 50)
        useCases.inspect(id, run.id)
        val repository = dependencies.runnerResultsRepository
        repository.records.toList().forEach {
            repository.setStatus(it.id, RunnerResultStatus.Dismissed)
        }
        val filter = RunnerResultFilter(showDismissed = false)
        val empty = useCases.dataset.page(id, run.id, 25, filter).page!!
        assertEquals(50, empty.savedCount)
        assertEquals(0, empty.visibleCount)
        assertEquals(0, empty.offset)
        assertNull(empty.nextOffset)
        assertEquals(1, remote.resultReads)
        remote.page = response(50, 25, null)
        val loaded = useCases.dataset.loadMore(id, run.id, filter = filter)
        assertEquals(50, remote.requestedOffset)
        assertEquals(75, loaded.run.nextResultOffset)
        assertEquals(75, loaded.page!!.savedCount)
        assertEquals(25, loaded.page!!.visibleCount)
        assertEquals((50..74).toList(), loaded.page!!.savedResults.map { it.position })
        assertEquals(2, remote.resultReads)
    }

    @Test
    fun changingFilterResetsPageAndDismissingTheLastVisibleRowClampsPaging() = runBlocking<Unit> {
        val id = runner()
        val run = dependencies.runnerExecutionUseCases.start(id)
        dependencies.runnerExecutionRepository.page = response(0, 26, null)
        val component = runOnUiThread {
            lifecycle.resume()
            DefaultRunnerRunsComponent(
                appComponentContext(),
                dependencies.connectionRunnersRepository.records.value.single(),
                {},
            )
        }
        await(component) { !it.loadingHistory }
        runOnUiThread { component.onSelect(run.id) }
        await(component) { it.view?.page?.visibleCount == 26 }
        runOnUiThread { component.onPage(25) }
        await(component) { it.view?.page?.offset == 25 }
        val filter = RunnerResultFilter(showDismissed = false)
        runOnUiThread { component.onFilter(filter) }
        await(component) { !it.reading && it.filter == filter && it.view?.page?.offset == 0 }
        runOnUiThread { component.onPage(25) }
        val last = await(component) {
            it.view?.page?.offset == 25
        }.view!!.page!!.savedResults.single()
        runOnUiThread { component.onResultAction(last.id, RunnerResultAction.Dismiss) }
        val updated = await(component) { !it.working && it.view?.page?.visibleCount == 25 }
        assertEquals(0, updated.view!!.page!!.offset)
        assertNull(updated.view!!.page!!.nextOffset)
        runOnUiThread { component.onFilter(RunnerResultFilter()) }
        val restored = await(component) { !it.reading && it.view?.page?.visibleCount == 26 }
        assertEquals(0, restored.view!!.page!!.offset)
        assertEquals(1, dependencies.runnerExecutionRepository.resultReads)
    }
}
