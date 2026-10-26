package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.vacancy.Vacancy
import reframecv.ui.context.AppComponentContext

/** Keeps export progress and retryable errors in the export dialog. */
enum class VacancyExportState { Idle, Exporting, Failed }

/** Selects printable vacancy content or portable JSON data. */
enum class VacancyExportFormat { Pdf, Json, Zip }

/** Lets the export dialog request a download and observe its progress. */
interface VacancyExportComponent {
    val state: Value<VacancyExportState>
    val format: Value<VacancyExportFormat>
    val archive: Value<VacancyArchiveDraft>
    fun onArchiveName(name: String)
    fun onResumeToggle(id: Long)
    fun onLetterToggle(id: Long)
    fun onRetryArchive()
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
    override val archive = MutableValue(VacancyArchiveDraft())
    private var loading: Job? = null
    override fun onFormatChange(format: VacancyExportFormat) {
        if (state.value != VacancyExportState.Exporting) {
            this.format.value = format
            state.value = VacancyExportState.Idle
            loading?.cancel()
            if (format == VacancyExportFormat.Zip) {
                vacancy()?.let { record ->
                    archive.value = VacancyArchiveDraft(
                        name = record.name,
                        invalidName = reframecv.export.archiveFilePrefix(record.name).isBlank(),
                    )
                    loadArchive(record)
                }
            }
        }
    }

    init {
        componentContext.lifecycle.doOnDestroy { scope.cancel() }
    }

    override fun onArchiveName(name: String) {
        if (state.value != VacancyExportState.Exporting) {
            archive.value = archive.value.copy(
                name = name,
                invalidName = reframecv.export.archiveFilePrefix(name).isBlank(),
            )
        }
    }
    override fun onResumeToggle(id: Long) {
        if (state.value == VacancyExportState.Exporting || archive.value.loading) return
        val draft = archive.value
        if (draft.resumes.any { it.id == id }) {
            archive.value = draft.copy(
                resumeIds = if (id in
                    draft.resumeIds
                ) {
                    draft.resumeIds - id
                } else {
                    draft.resumeIds + id
                },
            )
        }
    }
    override fun onLetterToggle(id: Long) {
        if (state.value == VacancyExportState.Exporting || archive.value.loading) return
        val draft = archive.value
        if (draft.letters.any { it.id == id }) {
            archive.value = draft.copy(
                letterIds = if (id in
                    draft.letterIds
                ) {
                    draft.letterIds - id
                } else {
                    draft.letterIds + id
                },
            )
        }
    }
    override fun onRetryArchive() {
        if (state.value == VacancyExportState.Exporting) return
        vacancy()?.let(::loadArchive)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadArchive(record: Vacancy) {
        loading?.cancel()
        archive.value = archive.value.copy(loading = true, failed = false)
        loading = scope.launch {
            try {
                val available = dependencies.vacancyArchiveUseCases.choices(record)
                archive.value = archive.value.copy(
                    resumes = available.resumes,
                    letters = available.coverLetters,
                    resumeIds = available.resumes.map { it.id }.toSet(),
                    letterIds = available.coverLetters.map { it.id }.toSet(),
                    loading = false,
                    loaded = true,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Load vacancy ZIP documents",
                    error,
                    mapOf("vacancyId" to record.id, "projectId" to record.projectId),
                )
                archive.value = archive.value.copy(loading = false, failed = true)
            }
        }
    }

    // Platform file operations can fail; cancellation still propagates to temporary-file cleanup.
    @Suppress("TooGenericExceptionCaught")
    override fun onDownload() {
        if (state.value == VacancyExportState.Exporting ||
            (format.value == VacancyExportFormat.Zip && !archive.value.ready)
        ) {
            return
        }
        val record = vacancy() ?: return
        val selected = format.value
        val draft = archive.value
        state.value = VacancyExportState.Exporting
        dependencies.eventHistory.record("Vacancy Export(vacancyId=${record.id})")
        scope.launch {
            try {
                when (selected) {
                    VacancyExportFormat.Pdf -> dependencies.vacancyFilesUseCases.generatePdf(record)

                    VacancyExportFormat.Json -> dependencies.vacancyFilesUseCases.export(record)

                    VacancyExportFormat.Zip -> dependencies.vacancyArchiveUseCases.export(
                        record,
                        draft.name,
                        draft.resumeIds,
                        draft.letterIds,
                    )
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
            loading?.cancel()
            format.value = VacancyExportFormat.Pdf
            archive.value = VacancyArchiveDraft()
        }
    }
}
