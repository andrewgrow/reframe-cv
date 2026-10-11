package reframecv.attachments

import reframecv.domain.models.importrecord.ImportRecord

/** Combines saved snapshot metadata with the current availability of its file. */
data class SnapshotInfo(val record: ImportRecord, val status: AttachmentStatus?)

fun ImportRecord.storedAttachment(): StoredAttachment? = filePath?.let {
    StoredAttachment(
        it,
        checkNotNull(fileName),
        checkNotNull(mediaType),
        checkNotNull(fileSize),
        checkNotNull(sha256),
    )
}
