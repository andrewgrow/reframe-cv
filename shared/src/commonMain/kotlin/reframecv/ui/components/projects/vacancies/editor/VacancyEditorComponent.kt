package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.domain.models.vacancy.Vacancy
import reframecv.recognition.RecognitionSource
import reframecv.recognition.RecognizedVacancy
import reframecv.recognition.applyTo
import reframecv.ui.components.common.section.CollapsibleSectionComponent
import reframecv.ui.components.common.section.DefaultCollapsibleSectionComponent
import reframecv.ui.context.AppComponentContext

/** Lets the vacancy editor show save progress and failures. */
enum class VacancySaveState { Idle, Saving, Failed }

/** Controls inline deletion confirmation, progress, and failure feedback. */
enum class VacancyDeleteState { Idle, Confirming, Deleting, Failed }

/** Lets the vacancy page create, edit, or confirm deletion of a vacancy. */
interface VacancyEditorComponent {
    val draft: Value<VacancyDraft>
    fun onDraftChange(draft: VacancyDraft)
    val details: CollapsibleSectionComponent
    val resumes: VacancyResumesComponent
    val snapshot: VacancySnapshotComponent
    val recognition: VacancyRecognitionComponent
    fun onRecognize()
    val initialVacancy: Vacancy?
    val deleteState: Value<VacancyDeleteState>
    val saveState: Value<VacancySaveState>
    val exportVisible: Value<Boolean>
    val importComponent: VacancyImportComponent
    fun onImport()
    val exportComponent: VacancyExportComponent
    fun onExport()
    fun onCloseExport()
    val isBusy: Boolean
        get() = saveState.value == VacancySaveState.Saving ||
            deleteState.value == VacancyDeleteState.Deleting ||
            exportComponent.state.value == VacancyExportState.Exporting ||
            importComponent.state.value == VacancyImportState.Importing ||
            recognition.dialog.value.component != null ||
            recognition.state.value == VacancyRecognitionState.Processing
    fun onSave(draft: VacancyDraft)
    fun onDelete()
    fun onClose()
}

/** Prevents untitled vacancies, accidental deletion, and dismissal during unfinished changes. */
class DefaultVacancyEditorComponent(
    componentContext: AppComponentContext,
    private val save: (VacancyDraft) -> Unit,
    private val close: () -> Unit,
    initialVacancy: Vacancy? = null,
    private val delete: () -> Unit = {},
    projectId: Long = initialVacancy?.projectId ?: 0,
) : VacancyEditorComponent,
    AppComponentContext by componentContext {
    override val details = DefaultCollapsibleSectionComponent(
        componentContext,
        "job_details",
        initialVacancy?.let { ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId) },
    )
    private var persistedVacancy = initialVacancy
    override val initialVacancy: Vacancy? get() = persistedVacancy
    override val draft =
        MutableValue(initialVacancy?.let(VacancyDraft::fromVacancy) ?: VacancyDraft(""))
    private val modalOpen get() = exportVisible.value || resumes.state.value.unlinkId != null
    override val resumes: DefaultVacancyResumesComponent = DefaultVacancyResumesComponent(
        componentContext,
        projectId,
        current = { draft.value.resumes },
        changed = { draft.value = draft.value.copy(resumes = it) },
        canEdit = {
            !isBusy && !exportVisible.value && deleteState.value == VacancyDeleteState.Idle
        },
    )
    override val snapshot: DefaultVacancySnapshotComponent = DefaultVacancySnapshotComponent(
        componentContext,
        initialVacancy?.id,
        initialVacancy?.vacancyAttachmentId != null,
        canEdit = { !isBusy },
        changed = { recognition.onReset() },
    )
    override val recognition: DefaultVacancyRecognitionComponent =
        DefaultVacancyRecognitionComponent(
            componentContext,
            source = {
                val state = snapshot.state.value
                state.selected?.let { RecognitionSource.Selected(it) }
                    ?: state.record?.let { RecognitionSource.Saved(it) }
            },
            recognized = ::applyRecognition,
            vacancyId = { this.initialVacancy?.id },
        )
    override fun onRecognize() {
        if (isBusy || modalOpen ||
            snapshot.state.value.loading
        ) {
            return
        }
        if (deleteState.value != VacancyDeleteState.Idle) return
        importComponent.onReset()
        recognition.onRecognize()
    }
    private fun applyRecognition(result: RecognizedVacancy) {
        snapshot.onParsed()
        val current = draft.value
        val original = current.applyTo(initialVacancy ?: current.toVacancy(0))
        draft.value =
            VacancyDraft.fromVacancy(
                result.applyTo(original),
            ).copy(keywords = current.keywords, snapshotChange = current.snapshotChange)
    }
    override val saveState = MutableValue(VacancySaveState.Idle)
    override val exportVisible = MutableValue(false)
    override val exportComponent = DefaultVacancyExportComponent(
        componentContext,
        vacancy = {
            this.initialVacancy?.takeIf { exportVisible.value }?.let { draft.value.applyTo(it) }
        },
        finished = ::onCloseExport,
    )
    override val importComponent = DefaultVacancyImportComponent(componentContext) {
        draft.value = VacancyDraft.fromVacancy(it).copy(
            snapshotChange = draft.value.snapshotChange,
            resumes = draft.value.resumes,
        )
    }
    override fun onImport() {
        if (initialVacancy != null) return
        if (!isBusy && !modalOpen && !snapshot.state.value.selecting) {
            saveState.value = VacancySaveState.Idle
            recognition.onReset()
            importComponent.onImport()
        }
    }
    private var exportAfterSave = false
    override val deleteState = MutableValue(VacancyDeleteState.Idle)
    override fun onDraftChange(draft: VacancyDraft) {
        if (!isBusy && !modalOpen &&
            deleteState.value == VacancyDeleteState.Idle
        ) {
            recognition.onReset()
            this.draft.value = draft
        }
    }

    override fun onSave(draft: VacancyDraft) = requestSave(draft, export = false)

    override fun onExport() {
        if (initialVacancy != null) requestSave(draft.value, export = true)
    }

    private fun requestSave(draft: VacancyDraft, export: Boolean) {
        if (isBusy || modalOpen ||
            snapshot.state.value.selecting
        ) {
            return
        }
        if (draft.name.isNotBlank() && deleteState.value == VacancyDeleteState.Idle) {
            exportAfterSave = export
            saveState.value = VacancySaveState.Saving
            save(draft.copy(snapshotChange = snapshot.change))
        }
    }

    internal fun onSaveCompleted(state: VacancySaveState, createdVacancy: Vacancy? = null) {
        if (createdVacancy != null) {
            persistedVacancy = createdVacancy
            details.onBindOwner(
                ScreenStateOwner(
                    ScreenType.Vacancy,
                    createdVacancy.id,
                    createdVacancy.projectId,
                ),
            )
        }
        saveState.value = state
        if (state == VacancySaveState.Idle) {
            snapshot.onSaved(createdVacancy?.id)
            importComponent.onReset()
            exportVisible.value = exportAfterSave
        }
        exportAfterSave = false
    }

    override fun onCloseExport() {
        if (!isBusy) {
            exportVisible.value = false
            exportComponent.onReset()
        }
    }

    override fun onDelete() {
        if (initialVacancy != null && !isBusy && !modalOpen) {
            if (deleteState.value == VacancyDeleteState.Idle) {
                deleteState.value = VacancyDeleteState.Confirming
            } else {
                delete()
            }
        }
    }

    override fun onClose() {
        if (resumes.state.value.unlinkId != null) {
            resumes.onCancelUnlink()
            return
        }
        if (recognition.dialog.value.component != null) {
            recognition.onCancel()
            return
        }
        if (!isBusy) {
            if (exportVisible.value) {
                onCloseExport()
            } else if (deleteState.value != VacancyDeleteState.Idle) {
                deleteState.value = VacancyDeleteState.Idle
            } else {
                importComponent.onClose()
                close()
            }
        }
    }
}
