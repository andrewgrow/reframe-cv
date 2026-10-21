package reframecv.ui.components.projects.coverletters

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import reframecv.dependencies.ApplicationDependencies
import reframecv.domain.models.coverletter.CoverLetter
import reframecv.repository.CoverLettersRepository
import reframecv.testing.ComponentTest
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread
import reframecv.usecase.CoverLettersUseCases

class CoverLetterSelectionComponentTest : ComponentTest() {
    private val records =
        listOf(CoverLetter(1, 7, "Main", 0, 0), CoverLetter(2, 7, "Adapted", 0, 0))
    private val state = MutableValue<CoverLettersState>(CoverLettersState.Ready(records))

    @Test
    fun selectsOnlyVisibleLettersSupportsSelectAllAndPrunesRemovedRecords() = runBlocking<Unit> {
        runOnUiThread {
            lifecycle.resume()
            val component =
                DefaultCoverLetterSelectionComponent(appComponentContext(), 7, state) { true }
            component.onToggle(999)
            assertTrue(component.state.value.selected.isEmpty())
            component.onToggle(1)
            assertEquals(setOf(1L), component.state.value.selected)
            component.onToggleAll()
            assertEquals(setOf(1L, 2L), component.state.value.selected)
            component.onToggleAll()
            assertTrue(component.state.value.selected.isEmpty())
            component.onToggle(2)
            component.onRequestDelete()
            component.onToggle(1)
            assertEquals(setOf(2L), component.state.value.selected)
            component.onCancelDelete()
            assertFalse(component.state.value.confirming)
            state.value = CoverLettersState.Ready(listOf(records.first()))
            assertTrue(component.state.value.selected.isEmpty())
        }
    }

    @Test
    fun failureKeepsSelectionAndConfirmationForRetryAndBusyStateBlocksDismissal() =
        runBlocking<Unit> {
            dependencies.coverLettersRepository.records.value = records
            var fail = true
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val delegate = dependencies.coverLettersRepository
            val repository = object : CoverLettersRepository by delegate {
                override suspend fun deleteMany(projectId: Long, ids: Set<Long>) {
                    if (fail) error("Database unavailable")
                    started.complete(Unit)
                    release.await()
                    dependencies.coverLettersRepository.deleteMany(projectId, ids)
                }
            }
            val failed = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()
            val component = runOnUiThread {
                lifecycle.resume()
                val context =
                    DefaultAppComponentContext(
                        com.arkivanov.decompose.DefaultComponentContext(lifecycle),
                        object : ApplicationDependencies by dependencies {
                            override val coverLettersUseCases = CoverLettersUseCases(repository)
                        },
                    )
                DefaultCoverLetterSelectionComponent(context, 7, state) { true }.also {
                    it.state.subscribe { value ->
                        if (value.failed) failed.complete(Unit)
                        if (started.isCompleted && !value.confirming) completed.complete(Unit)
                    }
                    it.onToggleAll()
                    it.onRequestDelete()
                    it.onConfirmDelete()
                }
            }
            withTimeout(5000) { failed.await() }
            runOnUiThread {
                assertTrue(component.state.value.confirming)
                assertEquals(setOf(1L, 2L), component.state.value.selected)
                assertEquals(2, dependencies.coverLettersRepository.records.value.size)
                fail = false
                component.onConfirmDelete()
            }
            withTimeout(5000) { started.await() }
            runOnUiThread {
                component.onCancelDelete()
                component.onToggleAll()
                assertTrue(component.state.value.deleting)
                assertTrue(component.state.value.confirming)
            }
            release.complete(Unit)
            withTimeout(5000) { completed.await() }
            assertTrue(dependencies.coverLettersRepository.records.value.isEmpty())
        }
}
