package reframecv.testing

import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.takahirom.roborazzi.captureRoboImage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.Rule
import org.junit.rules.TestName

/** Compares rasterized PDF pages, including pagination, through the shared golden workflow. */
abstract class PdfGoldenScreenshotTest {
    @get:Rule val testName = TestName()

    protected fun capturePdfGolden(bytes: ByteArray, expectedPages: Int) {
        val path = javaClass.name.removePrefix("reframecv.")
            .removeSuffix("ScreenshotTest").replace('.', '/')
        val report = Path.of("build/reports/pdf-goldens", path, "${testName.methodName}.pdf")
        Files.createDirectories(report.parent)
        Files.write(report, bytes)
        Loader.loadPDF(bytes).use { pdf ->
            assertEquals(expectedPages, pdf.numberOfPages, "PDF page count changed")
            val renderer = PDFRenderer(pdf)
            repeat(pdf.numberOfPages) { index ->
                renderer.renderImageWithDPI(index, 96f, ImageType.RGB)
                    .toComposeImageBitmap()
                    .captureRoboImage("$path/${testName.methodName}-page-${index + 1}.png")
            }
        }
    }
}
