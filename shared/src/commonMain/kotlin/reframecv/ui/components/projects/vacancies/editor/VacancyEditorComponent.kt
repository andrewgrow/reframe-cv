package reframecv.ui.components.projects.vacancies.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.domain.models.resume.Resume
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.domain.models.vacancy.Vacancy
import reframecv.domain.models.vacancy.VacancyResume
import reframecv.recognition.RecognitionSource
import reframecv.recognition.RecognizedVacancy
import reframecv.recognition.applyTo
import reframecv.ui.components.common.ai.AiPromptPreference
import reframecv.ui.components.common.section.CollapsibleSectionComponent
import reframecv.ui.components.common.section.DefaultCollapsibleSectionComponent
import reframecv.ui.context.AppComponentContext

/** Lets the vacancy editor show save progress and failures. */
enum class VacancySaveState { Idle, Saving, Failed, InvalidAppliedAt }

/** Controls inline deletion confirmation, progress, and failure feedback. */
enum class VacancyDeleteState { Idle, Confirming, Deleting, Failed }

/** Lets the vacancy page create, edit, or confirm deletion of a vacancy. */
interface VacancyEditorComponent {
    val draft: Value<VacancyDraft>
    fun onDraftChange(draft: VacancyDraft)
    val resumeSection: CollapsibleSectionComponent
    val letterSection: CollapsibleSectionComponent
    val letters: VacancyLettersComponent
    val letterGeneration: VacancyLetterGenerationComponent
    val details: CollapsibleSectionComponent
    val improvement: VacancyResumeImprovementComponent
    val assessment: VacancyResumeAssessmentComponent
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
            letterGeneration.dialog.value.component != null ||
            letterGeneration.selection.value.visible ||
            improvement.dialog.value.component != null ||
            assessment.dialog.value.component != null ||
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
    openResume: (Resume) -> Unit = {},
    openLetter: (CoverLetter) -> Unit = {},
) : VacancyEditorComponent,
    AppComponentContext by componentContext {
    override val resumeSection =
        DefaultCollapsibleSectionComponent(
            componentContext,
            "linked_resumes",
            initialVacancy?.let {
                ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId)
            },
        )
    override val letterSection =
        DefaultCollapsibleSectionComponent(
            componentContext,
            "linked_letters",
            initialVacancy?.let {
                ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId)
            },
        )
    private val letterPrompt =
        AiPromptPreference(
            componentContext,
            "cover_letter_prompt_id",
            initialVacancy?.let {
                ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId)
            },
        )
    override val details = DefaultCollapsibleSectionComponent(
        componentContext,
        "job_details",
        initialVacancy?.let { ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId) },
    )
    private val assessmentPrompt = AiPromptPreference(
        componentContext,
        "resume_assessment_prompt_id",
        initialVacancy?.let { ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId) },
    )
    private val improvementPrompt = AiPromptPreference(
        componentContext,
        "resume_improvement_prompt_id",
        initialVacancy?.let { ScreenStateOwner(ScreenType.Vacancy, it.id, it.projectId) },
    )
    private var persistedVacancy = initialVacancy
    override val initialVacancy: Vacancy? get() = persistedVacancy
    override val draft =
        MutableValue(initialVacancy?.let(VacancyDraft::fromVacancy) ?: VacancyDraft(""))
    private val modalOpen: Boolean get() = exportVisible.value ||
        resumes.state.value.unlinkId != null || letters.state.value.unlinkId != null ||
        assessment.selection.value.resumeId != null
    override val resumes: DefaultVacancyResumesComponent = DefaultVacancyResumesComponent(
        componentContext,
        projectId,
        current = { draft.value.resumes },
        open = openResume,
        changed = { draft.value = draft.value.copy(resumes = it) },
        canEdit = {
            !isBusy && !exportVisible.value && deleteState.value == VacancyDeleteState.Idle
        },
    )
    override val letters: VacancyLettersComponent = DefaultVacancyLettersComponent(
        componentContext,
        projectId,
        current = { draft.value.coverLetterIds },
        changed = { draft.value = draft.value.copy(coverLetterIds = it) },
        canEdit = {
            !isBusy && !exportVisible.value && deleteState.value == VacancyDeleteState.Idle
        },
        open = openLetter,
    )
    override val letterGeneration: VacancyLetterGenerationComponent =
        DefaultVacancyLetterGenerationComponent(
            componentContext,
            vacancy = {
                draft.value.applyTo(this.initialVacancy ?: draft.value.toVacancy(projectId))
            },
            resumes = { resumes.state.value.available },
            created = letters::onCreated,
            canEdit = {
                !isBusy && !modalOpen && deleteState.value == VacancyDeleteState.Idle
            },
            promptPreference = letterPrompt,
        )
    override val assessment: VacancyResumeAssessmentComponent =
        DefaultVacancyResumeAssessmentComponent(
            componentContext,
            vacancy = {
                draft.value.applyTo(this.initialVacancy ?: draft.value.toVacancy(projectId))
            },
            resumes = { resumes.state.value.available },
            changed = { draft.value = draft.value.copy(resumes = it) },
            canEdit = { !isBusy && !modalOpen && deleteState.value == VacancyDeleteState.Idle },
            promptPreference = assessmentPrompt,
        )
    override val improvement: VacancyResumeImprovementComponent =
        DefaultVacancyResumeImprovementComponent(
            componentContext,
            vacancy = {
                draft.value.applyTo(this.initialVacancy ?: draft.value.toVacancy(projectId))
            },
            resumes = { resumes.state.value.available },
            improved = { record ->
                resumes.onCreated(record)
                draft.value =
                    draft.value.copy(
                        resumes =
                            draft.value.resumes +
                                VacancyResume(record.id),
                    )
            },
            canEdit = { !isBusy && !modalOpen && deleteState.value == VacancyDeleteState.Idle },
            promptPreference = improvementPrompt,
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
            ).copy(
                applied = current.applied,
                appliedDate = current.appliedDate,
                keywords = current.keywords,
                snapshotChange = current.snapshotChange,
                coverLetterIds = current.coverLetterIds,
            )
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
            applied = draft.value.applied,
            appliedDate = draft.value.appliedDate,
            snapshotChange = draft.value.snapshotChange,
            resumes = draft.value.resumes,
            coverLetterIds = draft.value.coverLetterIds,
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
            if (saveState.value == VacancySaveState.InvalidAppliedAt && draft.validAppliedTime()) {
                saveState.value = VacancySaveState.Idle
            }
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
        if (!draft.validAppliedTime()) {
            saveState.value = VacancySaveState.InvalidAppliedAt
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
            val owner =
                ScreenStateOwner(ScreenType.Vacancy, createdVacancy.id, createdVacancy.projectId)
            assessmentPrompt.onBindOwner(owner)
            improvementPrompt.onBindOwner(owner)
            details.onBindOwner(owner)
            resumeSection.onBindOwner(owner)
            letterSection.onBindOwner(owner)
            letterPrompt.onBindOwner(owner)
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
        when {
            letterGeneration.dialog.value.component != null ||
                letterGeneration.selection.value.visible -> letterGeneration.onCancel()

            letters.state.value.unlinkId != null -> letters.onCancelUnlink()

            improvement.dialog.value.component != null -> improvement.onCancel()

            assessment.selection.value.resumeId != null -> assessment.onCloseResult()

            assessment.dialog.value.component != null -> assessment.onCancel()

            resumes.state.value.unlinkId != null -> resumes.onCancelUnlink()

            recognition.dialog.value.component != null -> recognition.onCancel()

            !isBusy -> {
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
}
