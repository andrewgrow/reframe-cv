package reframecv.ui.compose.application

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.database.buildDatabase
import reframecv.database.createDatabaseBuilder
import reframecv.domain.models.project.Project
import reframecv.domain.models.project.ProjectMode
import reframecv.domain.models.settings.WindowSettings
import reframecv.repository.LocalAppSettingsRepository
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.application.TestRootComponent
import reframecv.ui.components.application.navigation.ProjectTreeState
import reframecv.ui.components.projects.dashboard.TestDashboardComponent
import reframecv.ui.compose.projects.dashboard.dashboardSample

/** Verifies application layout using settings read after closing and reopening the database. */
@OptIn(ExperimentalTestApi::class)
class RestoredWindowScreenshotTest : GoldenScreenshotTest() {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultRestoredWindowMatchesReference() = captureRestoredWindow(WindowSettings())

    @Test
    fun compactRestoredWindowMatchesReference() = captureRestoredWindow(WindowSettings(640f, 480f))

    @Test
    fun wideRestoredWindowMatchesReference() = captureRestoredWindow(WindowSettings(1200f, 800f))

    @Test
    fun tallRestoredWindowMatchesReference() = captureRestoredWindow(WindowSettings(600f, 900f))

    private fun captureRestoredWindow(settings: WindowSettings) {
        val restored = runBlocking {
            val file = temporaryFolder.root.toPath().resolve("window.db")
            val database = buildDatabase(createDatabaseBuilder(file))
            try {
                LocalAppSettingsRepository(database.appSettingsDao()).updateWindowSettings(settings)
            } finally {
                database.close()
            }
            val reopened = buildDatabase(createDatabaseBuilder(file))
            try {
                LocalAppSettingsRepository(reopened.appSettingsDao()).getSettings().window
            } finally {
                reopened.close()
            }
        }
        assertEquals(settings, restored)
        val screen = ScreenSize(restored.width.dp, restored.height.dp)
        val root = TestRootComponent(
            dashboardComponent = TestDashboardComponent(initialState = dashboardSample()),
            initialTreeState = ProjectTreeState(
                projects = listOf(
                    Project(
                        id = 1,
                        name = "Backend",
                        createdAt = 0,
                        updatedAt = 0,
                        mode = ProjectMode.Workspace,
                    ),
                ),
                selectedId = 1,
                loading = false,
            ),
        )
        runDesktopComposeUiTest(screen) {
            setGoldenContent(this, screenSize = screen) { RootContent(root) }
            val bounds = onNodeWithTag("app.root").fetchSemanticsNode().boundsInRoot
            assertEquals(with(density) { screen.width.toPx() }, bounds.width)
            assertEquals(with(density) { screen.height.toPx() }, bounds.height)
            captureGolden(this, "app.root")
        }
    }
}
