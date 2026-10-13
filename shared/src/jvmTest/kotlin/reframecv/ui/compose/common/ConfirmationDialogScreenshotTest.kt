package reframecv.ui.compose.common

import androidx.compose.ui.test.ExperimentalTestApi
import kotlin.test.Test
import reframecv.shared.generated.resources.Res
import reframecv.shared.generated.resources.resume_cancel
import reframecv.shared.generated.resources.resume_create
import reframecv.shared.generated.resources.resume_delete_confirm
import reframecv.shared.generated.resources.resume_delete_error
import reframecv.shared.generated.resources.resume_delete_prompt
import reframecv.shared.generated.resources.resume_delete_title
import reframecv.shared.generated.resources.resume_delete_warning
import reframecv.shared.generated.resources.resume_name_hint
import reframecv.shared.generated.resources.resume_save
import reframecv.testing.GoldenScreenshotTest
import reframecv.ui.components.common.confirmation.ConfirmationDialogContent
import reframecv.ui.components.common.confirmation.ConfirmationDialogState
import reframecv.ui.components.common.confirmation.ConfirmationDialogTone
import reframecv.ui.theme.ReframeTheme.ThemeMode

@OptIn(ExperimentalTestApi::class)
class ConfirmationDialogScreenshotTest : GoldenScreenshotTest() {
    @Test
    fun standardConfirmationWithoutInputMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this, themeMode = ThemeMode.Light) {
            ConfirmationDialog(
                ConfirmationDialogContent(
                    Res.string.resume_create,
                    Res.string.resume_name_hint,
                    Res.string.resume_save,
                    Res.string.resume_cancel,
                    subject = "Mobile developer",
                ),
                ConfirmationDialogState(),
                {},
            )
        }
        captureGolden(this, "confirmation.dialog")
    }

    @Test
    fun criticalConfirmationWithInputAndFailureMatchesReference() = runDesktopComposeUiTest {
        setGoldenContent(this) {
            ConfirmationDialog(
                ConfirmationDialogContent(
                    Res.string.resume_delete_title,
                    Res.string.resume_delete_warning,
                    Res.string.resume_delete_confirm,
                    Res.string.resume_cancel,
                    inputLabel = Res.string.resume_delete_prompt,
                    tone = ConfirmationDialogTone.Critical,
                ),
                ConfirmationDialogState(input = "DELETE", error = Res.string.resume_delete_error),
                {
                },
            )
        }
        captureGolden(this, "confirmation.dialog")
    }
}
