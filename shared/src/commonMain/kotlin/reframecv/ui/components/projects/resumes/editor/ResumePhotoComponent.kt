package reframecv.ui.components.projects.resumes.editor

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import reframecv.ui.context.AppComponentContext

/** Supplies the current portrait preview and file availability without saving draft changes. */
data class ResumePhotoState(
    val path: String? = null,
    val bytes: ByteArray? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)

/** Lets the editor select or remove a portrait using an inline confirmation. */
interface ResumePhotoComponent {
    val state: Value<ResumePhotoState>
    val visible: Value<Boolean>
    val confirmingDelete: Value<Boolean>
    fun onOpen()
    fun onChoose()
    fun onRequestDelete()
    fun onCancelDelete()
    fun onConfirmDelete()
    fun onClose()
    fun onSaved()
}

/** Keeps prepared portrait copies owned by the draft until Save succeeds or the editor is destroyed. */
class DefaultResumePhotoComponent(
    componentContext: AppComponentContext,
    path: String?,
    private val changed: (String?) -> Unit,
) : ResumePhotoComponent {
    private val dependencies = componentContext.dependencies
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pending = mutableSetOf<String>()
    private var disposed = false
    override val state = MutableValue(ResumePhotoState(path))
    override val visible = MutableValue(false)
    override val confirmingDelete = MutableValue(false)

    init {
        componentContext.lifecycle.doOnDestroy {
            disposed = true
            scope.cancel()
            val files = pending.toList()
            pending.clear()
            val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Main)
            cleanup.launch {
                try {
                    files.forEach { discard(it) }
                } finally {
                    cleanup.cancel()
                }
            }
        }
        if (path != null) {
            runOperation("Load resume photo") {
                state.value = state.value.copy(bytes = dependencies.resumePhotos.read(path))
            }
        }
    }

    override fun onOpen() {
        if (!disposed && !state.value.loading) visible.value = true
    }

    override fun onChoose() = runOperation("Prepare resume photo") {
        val path = dependencies.resumePhotos.choose()
        if (path != null) {
            pending += path
            val bytes =
                checkNotNull(dependencies.resumePhotos.read(path)) { "Prepared photo is missing" }
            val previous = state.value.path
            state.value = ResumePhotoState(path, bytes, loading = true)
            changed(path)
            previous?.takeIf { it in pending && it != path }?.let {
                discard(it)
                pending.remove(it)
            }
        }
    }

    override fun onRequestDelete() {
        if (!state.value.loading && state.value.path != null) confirmingDelete.value = true
    }

    override fun onCancelDelete() {
        confirmingDelete.value = false
    }

    override fun onConfirmDelete() {
        if (!state.value.loading && confirmingDelete.value) {
            val previous = state.value.path
            state.value = ResumePhotoState()
            confirmingDelete.value = false
            changed(null)
            if (previous != null && previous in pending) {
                scope.launch {
                    discard(previous)
                    pending.remove(previous)
                }
            }
        }
    }

    override fun onClose() {
        if (!state.value.loading) {
            if (confirmingDelete.value) confirmingDelete.value = false else visible.value = false
        }
    }

    override fun onSaved() {
        state.value.path?.let { pending.remove(it) }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun runOperation(operation: String, action: suspend () -> Unit) {
        if (disposed || state.value.loading || confirmingDelete.value) return
        state.value = state.value.copy(loading = true, failed = false)
        scope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                dependencies.logger.error(operation, error, emptyMap())
                state.value = state.value.copy(failed = true)
            } finally {
                state.value = state.value.copy(loading = false)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun discard(path: String) {
        try {
            dependencies.resumePhotos.discard(path)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            dependencies.logger.error("Discard resume photo draft", error, emptyMap())
        }
    }
}
