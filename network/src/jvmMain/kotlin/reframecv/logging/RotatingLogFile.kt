package reframecv.logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE

/** Keeps recent diagnostics across launches while bounding their disk usage. */
class RotatingLogFile(
    private val file: Path,
    private val maxFileBytes: Int = DEFAULT_MAX_FILE_BYTES,
    private val archiveCount: Int = DEFAULT_ARCHIVE_COUNT,
) {
    init {
        require(maxFileBytes >= MAX_UTF8_CHARACTER_BYTES)
        require(archiveCount > 0)
    }

    fun append(entry: String) = synchronized(writeLock) {
        file.parent?.let(Files::createDirectories)
        limitExistingFile()
        val bytes = entry.toByteArray(Charsets.UTF_8)
        var offset = 0
        var size = if (Files.exists(file)) Files.size(file) else 0L
        while (offset < bytes.size) {
            // Keep ordinary entries together; oversized entries span bounded files.
            val entryNeedsOwnFile = offset == 0 && bytes.size <= maxFileBytes &&
                size + bytes.size > maxFileBytes
            if (size == maxFileBytes.toLong() || entryNeedsOwnFile) {
                rotate()
                size = 0
            }
            val end = utf8Boundary(
                bytes,
                minOf(bytes.size.toLong(), offset.toLong() + maxFileBytes - size).toInt(),
            )
            if (end == offset) {
                rotate()
                size = 0
                continue
            }
            Files.newOutputStream(file, CREATE, APPEND).use {
                it.write(bytes, offset, end - offset)
            }
            size += end - offset
            offset = end
        }
    }

    private fun rotate() {
        Files.deleteIfExists(archive(archiveCount))
        for (index in archiveCount - 1 downTo 1) {
            val source = archive(index)
            if (Files.exists(source)) Files.move(source, archive(index + 1), REPLACE_EXISTING)
        }
        if (Files.exists(file)) Files.move(file, archive(1), REPLACE_EXISTING)
    }

    private fun archive(index: Int): Path = file.resolveSibling("${file.fileName}.$index")

    private fun limitExistingFile() {
        if (!Files.exists(file) || Files.size(file) <= maxFileBytes) return
        val tail = Files.newInputStream(file).use {
            it.skipNBytes(Files.size(file) - maxFileBytes)
            it.readNBytes(maxFileBytes)
        }
        var start = 0
        while (start < tail.size && isContinuation(tail[start])) start++
        val temporary = Files.createTempFile(file.toAbsolutePath().parent, "log-rotation-", ".tmp")
        try {
            Files.newOutputStream(temporary).use { it.write(tail, start, tail.size - start) }
            Files.move(temporary, file, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun utf8Boundary(bytes: ByteArray, requestedEnd: Int): Int {
        var end = requestedEnd
        while (end < bytes.size && end > 0 && isContinuation(bytes[end])) end--
        return end
    }

    private fun isContinuation(byte: Byte) = byte.toInt() and UTF8_PREFIX_MASK == UTF8_CONTINUATION

    companion object {
        const val DEFAULT_MAX_FILE_BYTES = 10 * 1024 * 1024
        const val DEFAULT_ARCHIVE_COUNT = 5
        private const val MAX_UTF8_CHARACTER_BYTES = 4
        private const val UTF8_PREFIX_MASK = 0xC0
        private const val UTF8_CONTINUATION = 0x80
        private val writeLock = Any()
    }
}
