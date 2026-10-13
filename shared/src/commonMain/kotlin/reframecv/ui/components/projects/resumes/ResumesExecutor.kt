package reframecv.ui.components.projects.resumes

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import reframecv.logging.ApplicationLogger
import reframecv.repository.ResumesRepository
import reframecv.ui.components.projects.resumes.editor.ResumeDraft

/** Observes project resumes and keeps failed reads retryable. */
internal class ResumesExecutor(
    private val projectId: Long,
    private val repository: ResumesRepository,
    private val logger: ApplicationLogger,
) : CoroutineExecutor<ResumesIntent, Nothing, ResumesState, ResumesState, ResumesLabel>() {
    private var observation: Job? = null

    private var saving: Job? = null

    override fun executeIntent(intent: ResumesIntent) {
        when (intent) {
            ResumesIntent.Load -> observe()
            is ResumesIntent.Create -> save(intent.draft)
        }
    }

    // Storage failures must retain the draft and preserve coroutine cancellation.
    @Suppress("TooGenericExceptionCaught")
    private fun save(draft: ResumeDraft) {
        if (!draft.isValid || saving?.isActive == true) return
        publish(ResumesLabel.Saving)
        saving = scope.launch {
            try {
                repository.create(draft.toResume(projectId))
                publish(ResumesLabel.Saved)
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error("Create resume", error, mapOf("projectId" to projectId))
                publish(ResumesLabel.SaveFailed)
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
                repository.observeResumes(projectId).collect { dispatch(ResumesState.Ready(it)) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (error: Exception) {
                logger.error("Load resumes", error, mapOf("projectId" to projectId))
                dispatch(ResumesState.LoadFailed)
            }
        }
    }
}
