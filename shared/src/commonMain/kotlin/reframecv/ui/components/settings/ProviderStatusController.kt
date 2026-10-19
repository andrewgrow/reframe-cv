package reframecv.ui.components.settings

import com.arkivanov.decompose.value.MutableValue
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import reframecv.usecase.ProviderSettingsUseCases

/** Applies enable/disable changes immediately without saving unrelated draft fields. */
internal class ProviderStatusController(
    private val useCases: ProviderSettingsUseCases,
    private val scope: CoroutineScope,
    private val state: MutableValue<ProviderEditorState>,
    private val cancelRequests: () -> Unit,
) {
    @Suppress("TooGenericExceptionCaught")
    fun change(active: Boolean) {
        val previous = state.value.connection.active
        if (state.value.saving || state.value.changingActive || previous == active) return
        cancelRequests()
        val draft = state.value
        state.value = draft.copy(
            connection = draft.connection.copy(active = active),
            error = null,
            errorDetails = null,
            accountError = null,
            accountErrorDetails = null,
            balanceError = false,
            balanceErrorDetails = null,
            changingActive = draft.connection.id != 0L,
        )
        if (draft.connection.id == 0L) return
        scope.launch {
            try {
                useCases.setActive(draft.connection.id, active)
                state.value = state.value.copy(changingActive = false)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state.value = state.value.copy(
                    connection = state.value.connection.copy(active = previous),
                    changingActive = false,
                    error = SettingsError.Save,
                )
            }
        }
    }
}
