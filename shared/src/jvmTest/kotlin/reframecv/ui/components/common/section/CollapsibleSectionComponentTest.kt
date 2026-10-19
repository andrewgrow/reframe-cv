package reframecv.ui.components.common.section

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import reframecv.domain.models.screenstate.ScreenStateOwner
import reframecv.domain.models.screenstate.ScreenType
import reframecv.domain.models.vacancy.Vacancy
import reframecv.testing.ComponentTest
import reframecv.ui.components.projects.vacancies.editor.DefaultVacancyEditorComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySaveState
import reframecv.ui.context.DefaultAppComponentContext
import reframecv.ui.threading.runOnUiThread

class CollapsibleSectionComponentTest : ComponentTest() {
    private val owner = ScreenStateOwner(ScreenType.Vacancy, 12, 7)

    @Test
    fun restoresVisibilityAndPersistsTogglesAfterThePageIsDestroyed() = runBlocking {
        dependencies.screenStatesRepository.states.value =
            mapOf(owner to buildJsonObject { put("details", "collapsed") })
        val loaded = CompletableDeferred<Unit>()
        val childLifecycle = LifecycleRegistry()
        lateinit var section: DefaultCollapsibleSectionComponent
        runOnUiThread {
            childLifecycle.resume()
            section = DefaultCollapsibleSectionComponent(
                DefaultAppComponentContext(DefaultComponentContext(childLifecycle), dependencies),
                "details",
                owner,
            )
            section.state.subscribe { if (!it.loading) loaded.complete(Unit) }
        }
        withTimeout(5000) { loaded.await() }
        runOnUiThread {
            assertEquals(false, section.state.value.expanded)
            section.onToggle()
            childLifecycle.destroy()
        }
        assertEquals("expanded", dependencies.screenStateUseCases.load(owner)?.text("details"))
        val reopened = CompletableDeferred<Unit>()
        runOnUiThread {
            lifecycle.resume()
            section = DefaultCollapsibleSectionComponent(appComponentContext(), "details", owner)
            section.state.subscribe { if (!it.loading) reopened.complete(Unit) }
        }
        withTimeout(5000) { reopened.await() }
        runOnUiThread { assertEquals(true, section.state.value.expanded) }
    }

    @Test
    fun invalidFieldValuesUseDefaultsWithoutDiscardingOtherState() = runBlocking {
        runOnUiThread { lifecycle.resume() }
        val invalid = listOf(
            JsonNull,
            JsonPrimitive(true),
            JsonPrimitive(42),
            JsonPrimitive("unknown"),
            JsonArray(emptyList()),
        )
        for (value in invalid) {
            dependencies.screenStateUseCases.update(
                owner,
                buildJsonObject {
                    put("details", value)
                    put("selected_tab", "attachments")
                },
            )
            val loaded = CompletableDeferred<Unit>()
            lateinit var section: DefaultCollapsibleSectionComponent
            runOnUiThread {
                section =
                    DefaultCollapsibleSectionComponent(appComponentContext(), "details", owner)
                section.state.subscribe { if (!it.loading) loaded.complete(Unit) }
            }
            withTimeout(5000) { loaded.await() }
            runOnUiThread {
                assertEquals(true, section.state.value.expanded)
                section.onToggle()
            }
            val restored = dependencies.screenStateUseCases.load(owner)
            assertEquals("collapsed", restored?.text("details"))
            assertEquals("attachments", restored?.text("selected_tab"))
        }
    }

    @Test
    fun bindsDraftVisibilityToTheCreatedVacancyWithoutChangingSavedContent() = runBlocking {
        lateinit var editor: DefaultVacancyEditorComponent
        val created = Vacancy(id = 12, projectId = 7, name = "Role", createdAt = 0, updatedAt = 0)
        runOnUiThread {
            lifecycle.resume()
            editor =
                DefaultVacancyEditorComponent(appComponentContext(), save = {
                }, close = {}, projectId = 7)
            assertEquals(true, editor.details.state.value.expanded)
            editor.details.onToggle()
            assertEquals(emptyMap(), dependencies.screenStatesRepository.states.value)
            editor.onSaveCompleted(VacancySaveState.Idle, created)
        }
        assertEquals("collapsed", dependencies.screenStateUseCases.load(owner)?.text("job_details"))
        runOnUiThread {
            assertEquals(created, editor.initialVacancy)
            editor.details.onToggle()
        }
        assertEquals("expanded", dependencies.screenStateUseCases.load(owner)?.text("job_details"))
    }
}
