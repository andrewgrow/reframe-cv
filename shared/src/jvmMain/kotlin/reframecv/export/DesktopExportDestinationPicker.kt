package reframecv.export

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

/** Supplies a save destination without writing the generated document. */
fun interface ExportDestinationPicker {
    suspend fun pick(fileName: String): String?
}

/** Opens the native desktop save dialog with the generated document filename. */
class DesktopExportDestinationPicker(private val title: String = "Export resume") :
    ExportDestinationPicker {
    override suspend fun pick(fileName: String): String? = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE)
        try {
            dialog.file = fileName
            dialog.isVisible = true
            dialog.file?.let { Path.of(dialog.directory, it).toString() }
        } finally {
            dialog.dispose()
        }
    }
}
