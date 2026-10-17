package reframecv.attachments

import reframecv.domain.models.vacancyattachment.VacancyAttachment

/** Combines saved snapshot metadata with the current availability of its file. */
data class SnapshotInfo(val record: VacancyAttachment, val status: AttachmentStatus)

fun VacancyAttachment.storedAttachment() = StoredAttachment(
    filePath,
    fileName,
    mediaType,
    fileSize,
    sha256,
)
