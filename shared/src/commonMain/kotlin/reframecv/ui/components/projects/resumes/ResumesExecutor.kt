package reframecv.ui.components.projects.resumes

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.domain.models.resume.Resume
import reframecv.logging.ApplicationLogger
import reframecv.repository.MainResumeChange
import reframecv.ui.components.projects.resumes.editor.ResumeDraft
import reframecv.usecase.ResumesUseCases

/** Observes project resumes and keeps failed reads retryable. */
internal class ResumesExecutor(
    private val projectId: Long,
    private val useCases: ResumesUseCases,
    private val logger: ApplicationLogger,
) : CoroutineExecutor<ResumesIntent, Nothing, ResumesState, ResumesState, ResumesLabel>() {
    private var observation: Job? = null

    private var saving: Job? = null

    override fun executeIntent(intent: ResumesIntent) {
        when (intent) {
            ResumesIntent.Load -> observe()

            is ResumesIntent.Create -> save(
                intent.draft,
                mainResumeChange = intent.mainResumeChange,
            )

            is ResumesIntent.Delete -> delete(intent.id)

            is ResumesIntent.Update -> save(intent.draft, intent.resume, intent.mainResumeChange)
        }
    }

    // Storage failures must retain the draft and preserve coroutine cancellation.
    @Suppress("TooGenericExceptionCaught")
    private fun save(
        draft: ResumeDraft,
        original: Resume? = null,
        mainResumeChange: MainResumeChange,
    ) {
        if (!draft.isValid || saving?.isActive == true) return
        require(original == null || original.projectId == projectId)
        publish(ResumesLabel.Saving)
        saving = scope.launch {
            try {
                val record = original?.let(draft::applyTo) ?: draft.toResume(projectId)
                val created = useCases.save(record, mainResumeChange)
                publish(ResumesLabel.Saved(created))
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                val operation = if (original == null) "Create resume" else "Update resume"
                val context = if (original == null) {
                    mapOf("projectId" to projectId)
                } else {
                    mapOf("projectId" to projectId, "resumeId" to original.id)
                }
                logger.error(operation, error, context)
                publish(ResumesLabel.SaveFailed)
            }
        }
    }

    // Repository failures must keep the confirmation available for retry.
    @Suppress("TooGenericExceptionCaught")
    private fun delete(id: Long) {
        if (saving?.isActive == true) return
        publish(ResumesLabel.Deleting)
        saving = scope.launch {
            try {
                useCases.delete(id)
                publish(ResumesLabel.Deleted)
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error(
                    "Delete resume",
                    error,
                    mapOf(
                        "projectId" to projectId,
                        "resumeId" to id,
                    ),
                )
                publish(ResumesLabel.DeleteFailed)
            }
        }
    }

    // Storage failures must leave the page usable without swallowing cancellation.
    @Suppress("TooGenericExceptionCaught")
    private fun observe() {
        observation?.cancel()
        dispatch(ResumesState.Loading)
        observation = scope.launch {
            try {
                useCases.observe(projectId).collect {
                    dispatch(ResumesState.Ready(it.resumes, it.mainResumeId, it.linkedVacancies))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error("Load resumes", error, mapOf("projectId" to projectId))
                dispatch(ResumesState.LoadFailed)
            }
        }
    }
}
