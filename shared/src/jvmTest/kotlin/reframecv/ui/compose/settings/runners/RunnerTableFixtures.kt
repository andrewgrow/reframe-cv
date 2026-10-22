package reframecv.ui.compose.settings.runners

import com.arkivanov.decompose.value.MutableValue
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.project.Project
import reframecv.domain.models.providers.ConnectionRun
import reframecv.domain.models.providers.ConnectionRunState
import reframecv.domain.models.providers.RunnerResult
import reframecv.domain.models.providers.RunnerResultStatus
import reframecv.runners.RunnerResultsPage
import reframecv.ui.components.settings.runners.RunnerDraft
import reframecv.ui.components.settings.runners.RunnerResultAction
import reframecv.ui.components.settings.runners.RunnerRunsComponent
import reframecv.ui.components.settings.runners.RunnerRunsState
import reframecv.usecase.RunnerRunView

internal fun runnerTableFixture(): RunnerRunsState {
    val runner = RunnerDraft(
        name = "Example search",
        searchUrls = "https://www.linkedin.com/jobs/search/?keywords=kotlin",
    )
        .applyTo(RunnerDraft.newRunner(1)).copy(id = 1)
    val rows = (1..5).map(::fixtureResult)
    val columns = kotlinx.serialization.json.Json.parseToJsonElement(rows.last().rawResultAsJson)
        .let { it as kotlinx.serialization.json.JsonObject }.keys.toList()
    val run = ConnectionRun(
        id = 1, runnerId = 1, configurationSnapshot = runner.configuration,
        state = ConnectionRunState.Succeeded, externalId = "exampleRun1", startedAt = 1792684800000,
        datasetId = "exampleDataset1", nextResultOffset = 50, resultTotal = 75,
        resultColumns = columns,
        costUsd = 0.01,
    )
    return RunnerRunsState(
        runner,
        runs = listOf(run),
        loadingHistory = false,
        view = RunnerRunView(
            run,
            null,
            RunnerResultsPage(
                emptyList(),
                0,
                null,
                75,
                savedResults = rows,
                savedCount = rows.size,
            ),
        ),
        projects = listOf(Project(1, "Example project", 1, 1)),
    )
}

private fun fixtureJson(index: Int) = buildJsonObject {
    put("id", "10$index")
    put(
        "title",
        if (index ==
            3
        ) {
            "Senior Kotlin Multiplatform engineer for connected devices"
        } else {
            "Mobile developer $index"
        },
    )
    put("companyName", "Example Company $index")
    put(
        "descriptionText",
        "Build mobile applications with Kotlin. Work with QA and designers. " +
            "Maintain production services and improve reliability.\n\n" +
            "This description deliberately spans several lines.",
    )
    put("location", if (index == 4) "" else "Vienna, Austria")
    put("postedAt", "2026-10-22")
    put("salary", if (index == 1) "EUR 65,000–80,000" else "")
    put(
        "benefits",
        buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("Flexible hours"))
        },
    )
    put(
        "companyAddress",
        buildJsonObject {
            put("country", "AT")
            put("city", "Vienna")
        },
    )
    put("companyWebsite", "https://company$index.example.test")
    if (index == 5) put("additionalField", "Unknown fields are retained")
}

private fun fixtureResult(index: Int): RunnerResult {
    val json = fixtureJson(index)
    return RunnerResult(
        index.toLong(), 1, index - 1, "LinkedIn", "10$index", json.toString(), 100,
        status = when (index) {
            4 -> RunnerResultStatus.Dismissed
            5 -> RunnerResultStatus.Imported
            else -> RunnerResultStatus.New
        },
        vacancyId = if (index == 5) 9 else null,
        previousResultId = if (index in 2..3) index + 10L else null,
        previousStatus = if (index ==
            2
        ) {
            RunnerResultStatus.Imported
        } else if (index ==
            3
        ) {
            RunnerResultStatus.Dismissed
        } else {
            null
        },
        previousVacancyId = if (index == 2) 7 else null,
    )
}

internal class PreviewRunnerRunsComponent(initial: RunnerRunsState) : RunnerRunsComponent {
    override val state = MutableValue(initial)
    var offset: Int? = null
    var loads = 0
    var action: Pair<Long, RunnerResultAction>? = null
    override fun onLoadMore() {
        loads++
    }
    override fun onResultAction(id: Long, action: RunnerResultAction) {
        this.action = id to action
    }
    override fun onProject(id: Long?) = Unit
    override fun onStart() = Unit
    override fun onSelect(id: Long) = Unit
    override fun onRefresh() = Unit
    override fun onPage(offset: Int) {
        this.offset = offset
    }
    override fun onRetryHistory() = Unit
    override fun onBack() = Unit
}
