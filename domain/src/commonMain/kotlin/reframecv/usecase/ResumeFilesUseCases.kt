package reframecv.usecase

import reframecv.domain.models.resume.Resume
import reframecv.export.ResumeExporter
import reframecv.importing.ResumeImporter
import reframecv.photos.PhotoCrop
import reframecv.photos.ResumePhotos

/** Provides document exchange and portrait operations while editors own draft lifetimes. */
class ResumeFilesUseCases(
    private val importer: ResumeImporter,
    private val exporter: ResumeExporter,
    private val photos: ResumePhotos,
    private val pdfExporter: ResumeExporter,
) {
    suspend fun importDocument() = importer.pickAndImport()
    suspend fun generatePdf(resume: Resume) = pdfExporter.export(resume)
    suspend fun export(resume: Resume) = exporter.export(resume)
    suspend fun choosePhoto() = photos.choose()
    suspend fun copyPhotoSource(path: String) = photos.copySource(path)
    suspend fun renderPhoto(path: String, crop: PhotoCrop) = photos.render(path, crop)
    suspend fun readPhoto(path: String) = photos.read(path)
    suspend fun discardPhoto(path: String) = photos.discard(path)
}
