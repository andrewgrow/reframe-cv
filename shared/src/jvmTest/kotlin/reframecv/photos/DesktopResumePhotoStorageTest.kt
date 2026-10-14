package reframecv.photos

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import reframecv.attachments.SelectedAttachment

class DesktopResumePhotoStorageTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun preparesPrintDimensionsAndDpiWithoutChangingSourceAndDeletesItsOwnCopy() = runBlocking {
        val original = photoBytes()
        val source = folder.newFile("source.png").toPath()
        Files.write(source, original)
        val storage = DesktopResumePhotoStorage(folder.root.toPath())
        val path = storage.prepare(SelectedAttachment(source.toString(), "source.png"))
        assertTrue(path.startsWith("documents/resume-photos/"))
        val copy = folder.root.toPath().resolve(path)
        val image = ImageIO.read(copy.toFile())
        assertEquals(413, image.width)
        assertEquals(531, image.height)
        assertContentEquals(original, Files.readAllBytes(source))
        ImageIO.createImageInputStream(copy.toFile()).use { stream ->
            val reader = ImageIO.getImageReaders(stream).next()
            try {
                reader.input = stream
                val root = reader.getImageMetadata(0).getAsTree("javax_imageio_jpeg_image_1.0")
                    as IIOMetadataNode
                val jfif = root.getElementsByTagName("app0JFIF").item(0) as IIOMetadataNode
                assertEquals("1", jfif.getAttribute("resUnits"))
                assertEquals("300", jfif.getAttribute("Xdensity"))
                assertEquals("300", jfif.getAttribute("Ydensity"))
            } finally {
                reader.dispose()
            }
        }
        assertNotNull(storage.read(path))
        storage.delete(path)
        storage.delete(path)
        assertNull(storage.read(path))
        assertTrue(Files.exists(source))
    }

    @Test
    fun rejectsInvalidFilesAndEscapingPathsWithoutLeavingPreparedCopies() = runBlocking {
        val source = folder.newFile("invalid.png").toPath()
        Files.writeString(source, "not an image")
        val storage = DesktopResumePhotoStorage(folder.root.toPath())
        assertFailsWith<IllegalArgumentException> {
            storage.prepare(SelectedAttachment(source.toString(), "invalid.png"))
        }
        val photos = folder.root.toPath().resolve("documents/resume-photos")
        assertTrue(Files.list(photos).use { it.toList().isEmpty() })
        listOf(source.toString(), "../source.png", "documents/resume-photos/../../source.png")
            .forEach { path ->
                assertFailsWith<IllegalArgumentException> { storage.delete(path) }
            }
        assertEquals("not an image", Files.readString(source))
    }

    @Test
    fun landscapeImageIsCenterCroppedAndExifRotationAndMalformedMetadataAreHandled() {
        val source = folder.newFile("landscape.png")
        val image = BufferedImage(900, 400, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color.RED
            graphics.fillRect(0, 0, 900, 400)
            graphics.color = Color.BLUE
            graphics.fillRect(200, 0, 500, 400)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", source)
        val output = folder.newFile("prepared.jpg")
        PrintPortrait.write(source.toPath(), output.toPath())
        val portrait = ImageIO.read(output)
        assertTrue(Color(portrait.getRGB(10, 200)).blue > 240)
        assertTrue(Color(portrait.getRGB(400, 200)).blue > 240)
        val tiny = BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB)
        tiny.setRGB(0, 2, Color.RED.rgb)
        val rotated = orientPhoto(tiny, 6)
        assertEquals(3, rotated.width)
        assertEquals(2, rotated.height)
        assertEquals(Color.RED.rgb, rotated.getRGB(0, 0))
        assertEquals(1, exifOrientation(byteArrayOf()))
        val exif = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN)
        exif.put("Exif\u0000\u0000".encodeToByteArray())
        exif.putShort(6, 0x4949.toShort())
        exif.putInt(10, 8)
        exif.putShort(14, 1)
        exif.putShort(16, 274)
        exif.putShort(24, 6)
        assertEquals(6, exifOrientation(exif.array()))
        exif.putInt(10, Int.MAX_VALUE)
        assertEquals(1, exifOrientation(exif.array()))
    }
}
