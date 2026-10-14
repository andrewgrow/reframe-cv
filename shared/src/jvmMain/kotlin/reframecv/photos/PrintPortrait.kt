package reframecv.photos

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.metadata.IIOMetadataNode
import kotlin.math.roundToInt

/** Creates a 35-by-45-mm, 300-dpi JPEG with proportional center cropping. */
internal object PrintPortrait {
    const val WIDTH = 413
    const val HEIGHT = 531
    const val DPI = 300
    private const val QUALITY = 0.92f

    fun write(source: Path, destination: Path, crop: PhotoCrop = PhotoCrop()) {
        val input = read(source)
        val output = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = output.createGraphics()
        try {
            graphics.color = Color.WHITE
            graphics.fillRect(0, 0, WIDTH, HEIGHT)
            graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC,
            )
            val placement = crop.placement(
                input.width.toFloat(),
                input.height.toFloat(),
                WIDTH.toFloat(),
                HEIGHT.toFloat(),
            )
            graphics.drawImage(
                input,
                placement.left.roundToInt(),
                placement.top.roundToInt(),
                placement.width.roundToInt(),
                placement.height.roundToInt(),
                null,
            )
        } finally {
            graphics.dispose()
        }
        writeJpeg(output, destination)
    }

    internal fun read(source: Path): BufferedImage =
        ImageIO.createImageInputStream(source.toFile()).use { stream ->
            require(stream != null) { "Photo cannot be opened" }
            val readers = ImageIO.getImageReaders(stream)
            require(readers.hasNext()) { "Unsupported or invalid photo" }
            val reader = readers.next()
            try {
                reader.input = stream
                val orientation = photoOrientation(reader.getImageMetadata(0))
                orientPhoto(reader.read(0), orientation)
            } finally {
                reader.dispose()
            }
        }

    private fun writeJpeg(image: BufferedImage, destination: Path) {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        try {
            ImageIO.createImageOutputStream(destination.toFile()).use { stream ->
                writer.output = stream
                val parameters = writer.defaultWriteParam
                parameters.compressionMode = ImageWriteParam.MODE_EXPLICIT
                parameters.compressionQuality = QUALITY
                val metadata = writer.getDefaultImageMetadata(
                    javax.imageio.ImageTypeSpecifier(image),
                    parameters,
                )
                val tree = metadata.getAsTree("javax_imageio_jpeg_image_1.0")
                val jfif = (tree as IIOMetadataNode).getElementsByTagName(
                    "app0JFIF",
                ).item(0) as IIOMetadataNode
                jfif.setAttribute("resUnits", "1")
                jfif.setAttribute("Xdensity", DPI.toString())
                jfif.setAttribute("Ydensity", DPI.toString())
                metadata.setFromTree("javax_imageio_jpeg_image_1.0", tree)
                writer.write(null, IIOImage(image, null, metadata), parameters)
            }
        } finally {
            writer.dispose()
        }
    }
}
