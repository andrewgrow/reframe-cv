package reframecv.export.fixtures

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

internal fun printableResumePortrait(): ByteArray {
    val image = BufferedImage(413, 531, BufferedImage.TYPE_INT_RGB)
    image.createGraphics().apply {
        color = Color(230, 235, 242)
        fillRect(0, 0, 413, 531)
        color = Color(70, 90, 120)
        fillOval(125, 90, 160, 180)
        fillOval(60, 280, 290, 320)
        dispose()
    }
    return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
}
