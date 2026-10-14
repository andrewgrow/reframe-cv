package reframecv.photos

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

fun photoBytes(color: Color = Color.BLUE, width: Int = 700, height: Int = 900): ByteArray {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.color = color
        graphics.fillRect(0, 0, width, height)
    } finally {
        graphics.dispose()
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
}

fun framingPhotoBytes(): ByteArray {
    val image = BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB)
    val graphics = image.createGraphics()
    try {
        listOf(Color(230, 160, 120), Color(160, 200, 180), Color(120, 160, 220))
            .forEachIndexed { index, color ->
                graphics.color = color
                graphics.fillRect(index * 300, 0, 300, 600)
            }
        graphics.color = Color.WHITE
        graphics.stroke = java.awt.BasicStroke(4f)
        graphics.drawOval(350, 200, 200, 200)
        graphics.drawLine(450, 0, 450, 600)
        graphics.drawLine(0, 300, 900, 300)
    } finally {
        graphics.dispose()
    }
    return ByteArrayOutputStream().use {
        ImageIO.write(image, "png", it)
        it.toByteArray()
    }
}
