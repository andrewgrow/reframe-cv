package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.context.AppComponentContext

/** Keeps export progress and retryable errors in the export dialog. */
enum class VacancyExportState { Idle, Exporting, Failed }

/** Lets the export dialog request a download and observe its progress. */
interface VacancyExportComponent {
    val state: Value<VacancyExportState>
    fun onDownload()
    fun onReset()
}

/** Runs document generation and file selection without blocking or losing the saved vacancy. */
class DefaultVacancyExportComponent(
    componentContext: AppComponentContext,
    private val vacancy: () -> Vacancy?,
    private val finished: () -> Unit,
) : VacancyExportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override val state = MutableValue(VacancyExportState.Idle)

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        if (state.value == VacancyExportState.Exporting) return
        val record = vacancy() ?: return
        state.value = VacancyExportState.Exporting
        dependencies.eventHistory.record("Vacancy Export(vacancyId=${record.id})")
        scope.launch {
            try {
                dependencies.vacancyFilesUseCases.export(record)
                state.value = VacancyExportState.Idle
                finished()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Export vacancy as JSON",
                    error,
                    mapOf("projectId" to record.projectId, "vacancyId" to record.id),
                )
                state.value = VacancyExportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value != VacancyExportState.Exporting) state.value = VacancyExportState.Idle
    }
}
