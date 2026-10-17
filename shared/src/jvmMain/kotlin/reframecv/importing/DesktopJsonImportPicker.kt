package reframecv.importing

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

/** Supplies an import source without modifying the user's original file. */
fun interface JsonImportPicker {
    suspend fun pick(): String?
}

/** Opens the native desktop file picker for JSON documents. */
class DesktopJsonImportPicker(private val title: String) : JsonImportPicker {
    override suspend fun pick(): String? = withContext(Dispatchers.Swing) {
        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
        try {
            dialog.setFilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
            dialog.isVisible = true
            dialog.file?.let { Path.of(dialog.directory, it).toString() }
        } finally {
            dialog.dispose()
        }
    }
}
