package reframecv.ui.components.settings.runners

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import reframecv.domain.models.providers.ApiProviderType
import reframecv.domain.models.providers.ApifyConfiguration
import reframecv.domain.models.providers.ProviderConnection
import reframecv.domain.models.providers.RunnerRawResult
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.runners.RunnerResultsPage
import reframecv.runners.VacancySearchResult
import reframecv.testing.ComponentTest

class RunnerResultPersistenceTest : ComponentTest() {
    @Test
    fun inspectionStoresRawRowsOnceAndNextRunReportsEarlierReview() = runBlocking<Unit> {
        val owner = dependencies.providerSettingsUseCases.save(
            ProviderConnection(
                type = ApiProviderType.Apify,
                name = "Example",
                configuration = ApifyConfiguration("fictional-token"),
            ),
        )
        val runner = dependencies.connectionRunnersUseCases.save(
            RunnerDraft(searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin")
                .applyTo(RunnerDraft.newRunner(owner)),
        )
        val raw = """{"id":"101","title":"Example role","extra":[1,2]}"""
        dependencies.runnerExecutionRepository.page = RunnerResultsPage(
            listOf(VacancySearchResult("101", "Example role", url = "https://example.test/job")),
            0,
            null,
            1,
            rawResults = listOf(RunnerRawResult(0, "LinkedIn", "101", raw)),
        )
        val useCases = dependencies.runnerExecutionUseCases
        val first = useCases.start(runner)
        val saved = useCases.inspect(runner, first.id).page!!.savedResults.single()
        assertEquals(raw, saved.rawResultAsJson)
        assertEquals(first.id, saved.runId)
        assertNull(saved.previousResultId)
        assertEquals(saved.id, useCases.inspect(runner, first.id).page!!.savedResults.single().id)
        dependencies.runnerResultsRepository.setStatus(saved.id, RunnerResultStatus.Dismissed)
        val second = useCases.start(runner)
        val repeated = useCases.inspect(runner, second.id).page!!.savedResults.single()
        assertEquals(saved.id, repeated.previousResultId)
        assertEquals(RunnerResultStatus.Dismissed, repeated.previousStatus)
        assertEquals(2, dependencies.runnerResultsRepository.records.size)
    }
}
