package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.domain.models.vacancy.Vacancy
import reframecv.importing.ImportedVacancyFile
import reframecv.ui.context.AppComponentContext

/** Reports import progress and errors without saving or discarding the current draft. */
enum class VacancyImportState { Idle, Importing, Imported, Failed }

/** Lets the creation page import fields and release temporary sources on close. */
interface VacancyImportComponent {
    val state: Value<VacancyImportState>
    fun onImport()
    fun onReset()
    fun onClose()
}

/** Owns imported temporary files for the lifetime of the vacancy creation page. */
class DefaultVacancyImportComponent(
    componentContext: AppComponentContext,
    private val imported: (Vacancy) -> Unit,
) : VacancyImportComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val files = mutableListOf<ImportedVacancyFile>()
    private var closed = false
    override val state = MutableValue(VacancyImportState.Idle)

    init {
        componentContext.lifecycle.doOnDestroy(::onClose)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onImport() {
        if (closed || state.value == VacancyImportState.Importing) return
        state.value = VacancyImportState.Importing
        dependencies.eventHistory.record("Vacancy Import(create)")
        scope.launch {
            try {
                val file = dependencies.vacancyImporter.pickAndImport()
                if (file == null) {
                    state.value = VacancyImportState.Idle
                } else {
                    files += file
                    imported(file.vacancy)
                    state.value = VacancyImportState.Imported
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(
                    "Import vacancy from JSON",
                    error,
                    mapOf("mode" to "create"),
                )
                state.value = VacancyImportState.Failed
            }
        }
    }

    override fun onReset() {
        if (state.value != VacancyImportState.Importing) state.value = VacancyImportState.Idle
    }

    @Suppress("TooGenericExceptionCaught")
    override fun onClose() {
        if (closed) return
        closed = true
        scope.cancel()
        val pending = files.toList()
        files.clear()
        CoroutineScope(NonCancellable + Dispatchers.Main).launch {
            pending.forEach { file ->
                try {
                    file.deleteTemporaryFile()
                } catch (error: Exception) {
                    dependencies.logger.error(
                        "Delete imported vacancy temporary file",
                        error,
                        emptyMap(),
                    )
                }
            }
        }
    }
}
