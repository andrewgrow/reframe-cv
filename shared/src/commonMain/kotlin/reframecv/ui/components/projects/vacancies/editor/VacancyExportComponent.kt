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

/** Selects printable vacancy content or portable JSON data. */
enum class VacancyExportFormat { Pdf, Json }

/** Lets the export dialog request a download and observe its progress. */
interface VacancyExportComponent {
    val state: Value<VacancyExportState>
    val format: Value<VacancyExportFormat>
    fun onFormatChange(format: VacancyExportFormat)
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
    override val format = MutableValue(VacancyExportFormat.Pdf)
    override fun onFormatChange(format: VacancyExportFormat) {
        if (state.value != VacancyExportState.Exporting) {
            this.format.value = format
            state.value = VacancyExportState.Idle
        }
    }

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        if (state.value == VacancyExportState.Exporting) return
        val record = vacancy() ?: return
        val selected = format.value
        state.value = VacancyExportState.Exporting
        dependencies.eventHistory.record("Vacancy Export(vacancyId=${record.id})")
        scope.launch {
            try {
                when (selected) {
                    VacancyExportFormat.Pdf -> dependencies.vacancyFilesUseCases.generatePdf(record)
                    VacancyExportFormat.Json -> dependencies.vacancyFilesUseCases.export(record)
                }
                state.value = VacancyExportState.Idle
                finished()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Export vacancy as ${selected.name.uppercase()}",
                    error,
                    mapOf("projectId" to record.projectId, "vacancyId" to record.id),
                )
                state.value = VacancyExportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value != VacancyExportState.Exporting) {
            state.value = VacancyExportState.Idle
            format.value = VacancyExportFormat.Pdf
        }
    }
}
