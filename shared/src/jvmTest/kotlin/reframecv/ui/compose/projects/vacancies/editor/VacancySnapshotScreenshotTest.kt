package reframecv.ui.compose.projects.vacancies.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import reframecv.attachments.AttachmentStatus
import reframecv.attachments.SelectedAttachment
import reframecv.domain.models.vacancyattachment.VacancyAttachment
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.projects.vacancies.editor.TestVacancySnapshotComponent
import reframecv.ui.components.projects.vacancies.editor.VacancySnapshotState
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class VacancySnapshotScreenshotTest : GoldenScreenshotTest() {
    @Test fun selectedFile() = capture(
        VacancySnapshotState(
            selected = SelectedAttachment("unused", "Mobile App Senior Developer.pdf"),
        ),
    )

    @Test fun savedFile() = capture(
        VacancySnapshotState(
            record = record(),
            status = AttachmentStatus.Available,
        ),
    )

    @Test fun missingFileInLightTheme() = capture(
        VacancySnapshotState(
            record = record(),
            status = AttachmentStatus.Missing,
        ),
        ThemeMode.Light,
    )

    @Test fun changedFile() = capture(
        VacancySnapshotState(
            record = record(),
            status = AttachmentStatus.Changed,
        ),
    )

    @Test fun failedOperation() = capture(VacancySnapshotState(failed = true))

    private fun record() = VacancyAttachment(
        filePath = "attachments/source.pdf",
        mediaType = "application/pdf",
        fileSize = 100,
        sha256 = "hash",
        createdAt = 100,
        fileName = "Mobile App Senior Developer.pdf",
    )

    private fun capture(state: VacancySnapshotState, theme: ThemeMode = ThemeMode.Dark) {
        val size = ScreenSize(500.dp, 240.dp)
        runDesktopComposeUiTest(size) {
            val component = TestVacancySnapshotComponent()
            component.state.value = state
            setAndCaptureGolden(this, size, themeMode = theme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize().padding(16.dp)) {
                        VacancySnapshotContent(component)
                    }
                }
            }
        }
    }
}
