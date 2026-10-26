package reframecv.export

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.time.Clock
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import reframecv.documents.coverletter.CoverLetterPrintData
import reframecv.documents.coverletter.DesktopCoverLetterPdfRenderer
import reframecv.documents.resume.DesktopResumePdfRenderer
import reframecv.documents.vacancy.DesktopVacancyPdfRenderer
import reframecv.photos.ResumePhotos

/** Packages the same printable PDFs as individual exports into one temporary ZIP. */
class DesktopVacancyArchiveExporter(
    private val photos: ResumePhotos,
    picker: ExportDestinationPicker = DesktopExportDestinationPicker("Save vacancy ZIP"),
    private val clock: Clock = Clock.systemDefaultZone(),
    temporaryDirectory: Path? = null,
) : VacancyArchiveExporter {
    private val files = DesktopDocumentExport(picker, clock, temporaryDirectory)
    override suspend fun export(archive: VacancyArchive): DocumentExportResult {
        val prefix = archiveFilePrefix(archive.name)
        require(prefix.isNotBlank())
        return files.export("vacancy", archive.vacancy.id, "zip", "$prefix.zip") {
            val bytes = ByteArrayOutputStream()
            ZipOutputStream(bytes).use { zip ->
                currentCoroutineContext().ensureActive()
                zip.addPdf(
                    "$prefix-vacancy.pdf",
                    DesktopVacancyPdfRenderer.render(archive.vacancy.toPrintData()),
                )
                archive.resumes.forEachIndexed { index, resume ->
                    currentCoroutineContext().ensureActive()
                    val portrait = if (resume.photoVisible) {
                        resume.photoPath?.let { photos.read(it) }
                    } else {
                        null
                    }
                    zip.addPdf(
                        "$prefix-resume-${index + 1}.pdf",
                        DesktopResumePdfRenderer.render(resume.toPrintData(portrait)),
                    )
                }
                archive.coverLetters.forEachIndexed { index, letter ->
                    currentCoroutineContext().ensureActive()
                    zip.addPdf(
                        "$prefix-coverLetter-${index + 1}.pdf",
                        DesktopCoverLetterPdfRenderer.render(CoverLetterPrintData(letter.content)),
                    )
                }
                currentCoroutineContext().ensureActive()
            }
            bytes.toByteArray()
        }
    }
    private fun ZipOutputStream.addPdf(name: String, content: ByteArray) {
        putNextEntry(ZipEntry(name).also { it.time = clock.millis() })
        write(content)
        closeEntry()
    }
}
