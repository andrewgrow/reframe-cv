package reframecv.attachments

class TestAttachmentStorage : AttachmentStorage {
    val files = mutableMapOf<String, StoredAttachment>()
    override suspend fun store(selection: SelectedAttachment): StoredAttachment {
        val attachment = StoredAttachment(
            "attachments/${files.size}.txt",
            selection.fileName,
            "text/plain",
            0,
            "test",
        )
        files[attachment.filePath] = attachment
        return attachment
    }
    override suspend fun status(attachment: StoredAttachment) =
        if (attachment.filePath in files) AttachmentStatus.Available else AttachmentStatus.Missing
    override suspend fun open(filePath: String) = Unit
    override suspend fun delete(filePath: String) {
        files.remove(filePath)
    }
    override suspend fun cleanup(referencedPaths: Set<String>) {
        files.keys.retainAll(referencedPaths)
    }
}
