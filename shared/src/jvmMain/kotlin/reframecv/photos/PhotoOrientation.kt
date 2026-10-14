package reframecv.photos

import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode

private const val EXIF_MARKER = 225
private const val ORIENTATION_TAG = 274
private const val MAX_EXIF_ORIENTATION = 8
private const val TIFF_HEADER_SIZE = 8
private const val IFD_ENTRY_SIZE = 12

internal fun photoOrientation(metadata: IIOMetadata): Int {
    if (metadata.nativeMetadataFormatName != "javax_imageio_jpeg_image_1.0") return 1
    val root = metadata.getAsTree(metadata.nativeMetadataFormatName) as IIOMetadataNode
    val markers = root.getElementsByTagName("unknown")
    val exif = (0 until markers.length).map { markers.item(it) as IIOMetadataNode }
        .find { it.getAttribute("MarkerTag").toIntOrNull() == EXIF_MARKER }
        ?.userObject as? ByteArray
    return exif?.let(::exifOrientation) ?: 1
}

private val exifHeader = "Exif\u0000\u0000".encodeToByteArray()
private const val UNSIGNED_SHORT = 0xffff
private const val LITTLE_ENDIAN_MARKER = 0x4949
private const val IFD_OFFSET_POSITION = 4
private const val ENTRY_VALUE_POSITION = 8

internal fun exifOrientation(bytes: ByteArray): Int {
    val tiff = exifTiff(bytes) ?: return 1
    val offset = tiff.getInt(IFD_OFFSET_POSITION)
    val count = if (offset in TIFF_HEADER_SIZE..(tiff.limit() - Short.SIZE_BYTES)) {
        tiff.getShort(offset).toInt() and UNSIGNED_SHORT
    } else {
        0
    }
    return (0 until count).asSequence()
        .map { offset.toLong() + Short.SIZE_BYTES + it.toLong() * IFD_ENTRY_SIZE }
        .takeWhile { it + IFD_ENTRY_SIZE <= tiff.limit() }
        .firstOrNull { tiff.getShort(it.toInt()).toInt() and UNSIGNED_SHORT == ORIENTATION_TAG }
        ?.let { tiff.getShort(it.toInt() + ENTRY_VALUE_POSITION).toInt() and UNSIGNED_SHORT }
        ?.takeIf { it in 1..MAX_EXIF_ORIENTATION } ?: 1
}

private fun exifTiff(bytes: ByteArray): ByteBuffer? {
    val headerSize = exifHeader.size
    if (bytes.size < headerSize + TIFF_HEADER_SIZE ||
        !bytes.copyOfRange(0, headerSize).contentEquals(exifHeader)
    ) {
        return null
    }
    val tiff = ByteBuffer.wrap(bytes, headerSize, bytes.size - headerSize).slice()
    tiff.order(
        if (tiff.getShort(0).toInt() ==
            LITTLE_ENDIAN_MARKER
        ) {
            ByteOrder.LITTLE_ENDIAN
        } else {
            ByteOrder.BIG_ENDIAN
        },
    )
    return tiff
}

internal fun orientPhoto(image: BufferedImage, orientation: Int): BufferedImage {
    if (orientation == 1) return image
    val width = image.width.toDouble()
    val height = image.height.toDouble()
    val transform = when (orientation) {
        2 -> AffineTransform(-1.0, 0.0, 0.0, 1.0, width, 0.0)
        3 -> AffineTransform(-1.0, 0.0, 0.0, -1.0, width, height)
        4 -> AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, height)
        5 -> AffineTransform(0.0, 1.0, 1.0, 0.0, 0.0, 0.0)
        6 -> AffineTransform(0.0, 1.0, -1.0, 0.0, height, 0.0)
        7 -> AffineTransform(0.0, -1.0, -1.0, 0.0, height, width)
        8 -> AffineTransform(0.0, -1.0, 1.0, 0.0, 0.0, width)
        else -> AffineTransform()
    }
    val swapped = orientation in 5..8
    val output = BufferedImage(
        if (swapped) image.height else image.width,
        if (swapped) image.width else image.height,
        BufferedImage.TYPE_INT_ARGB,
    )
    val graphics = output.createGraphics()
    try {
        graphics.drawImage(image, transform, null)
    } finally {
        graphics.dispose()
    }
    return output
}
