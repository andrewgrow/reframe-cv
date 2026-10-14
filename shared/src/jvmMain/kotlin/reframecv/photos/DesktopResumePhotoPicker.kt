package reframecv.photos

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import reframecv.attachments.SelectedAttachment

/** Opens native image selection for resume portraits. */
class DesktopResumePhotoPicker : ResumePhotoPicker {
    override suspend fun pick(): SelectedAttachment? = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, "Choose resume photo", FileDialog.LOAD)
        try {
            dialog.setFilenameFilter { _, name ->
                name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png")
            }
            dialog.isVisible = true
            dialog.file?.let { name ->
                SelectedAttachment(Path.of(dialog.directory, name).toString(), name)
            }
        } finally {
            dialog.dispose()
        }
    }
}
