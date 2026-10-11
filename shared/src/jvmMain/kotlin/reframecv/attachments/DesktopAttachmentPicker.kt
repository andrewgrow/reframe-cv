package reframecv.attachments

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

/** Lets desktop users choose a vacancy snapshot without copying it yet. */
class DesktopAttachmentPicker : AttachmentPicker {
    override suspend fun pick(): SelectedAttachment? = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, "Attach source file", FileDialog.LOAD)
        try {
            dialog.setFilenameFilter { _, name ->
                name.substringAfterLast('.', "").lowercase() in EXTENSIONS
            }
            dialog.isVisible = true
            dialog.file?.let { name ->
                SelectedAttachment(Path.of(dialog.directory, name).toString(), name)
            }
        } finally {
            dialog.dispose()
        }
    }

    companion object {
        private val EXTENSIONS =
            setOf("pdf", "docx", "txt", "png", "jpg", "jpeg", "json", "html", "htm")
    }
}
