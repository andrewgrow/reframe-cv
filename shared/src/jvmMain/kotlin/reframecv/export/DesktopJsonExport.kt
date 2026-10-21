package reframecv.export

import java.nio.file.Path
import java.time.Clock

/** Adapts JSON serialization to the shared temporary-document export lifecycle. */
internal class DesktopJsonExport(
    picker: ExportDestinationPicker,
    clock: Clock,
    temporaryDirectory: Path?,
) {
    private val files = DesktopDocumentExport(picker, clock, temporaryDirectory)

    suspend fun export(kind: String, id: Long, content: () -> String): DocumentExportResult =
        files.export(kind, id, "json") { content().toByteArray(Charsets.UTF_8) }
}
