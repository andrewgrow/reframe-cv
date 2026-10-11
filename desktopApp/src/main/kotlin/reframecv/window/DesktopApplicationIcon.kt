package reframecv.window

import java.awt.Taskbar
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/** Provides the packaged icon for desktop windows and the Dock or taskbar. */
internal object DesktopApplicationIcon {
    val image: BufferedImage by lazy {
        val resource = checkNotNull(javaClass.getResourceAsStream("/icons/reframe-cv.png"))
        resource.use { checkNotNull(ImageIO.read(it)) }
    }

    fun applyToTaskbar() {
        if (Taskbar.isTaskbarSupported()) {
            val taskbar = Taskbar.getTaskbar()
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.iconImage = image
            }
        }
    }
}
