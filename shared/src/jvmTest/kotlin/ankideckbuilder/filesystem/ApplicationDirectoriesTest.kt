package ankideckbuilder.filesystem

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationDirectoriesTest {
    private val home = Path.of("/users/test")

    @Test
    fun macUsesApplicationSupport() {
        assertEquals(
            home.resolve("Library/Application Support/AnkiDeckBuilder"),
            resolveApplicationDataDirectory(home, "Mac OS X", "/ignored", "/ignored"),
        )
    }

    @Test
    fun windowsUsesAppData() {
        assertEquals(
            Path.of("/roaming/AnkiDeckBuilder"),
            resolveApplicationDataDirectory(home, "Windows 11", "/roaming", null),
        )
    }

    @Test
    fun windowsFallsBackWhenAppDataIsMissingOrBlank() {
        for (value in listOf(null, "", "  ")) {
            assertEquals(
                home.resolve("AppData/Roaming/AnkiDeckBuilder"),
                resolveApplicationDataDirectory(home, "Windows 11", value, null),
            )
        }
    }

    @Test
    fun linuxUsesXdgDataHome() {
        assertEquals(
            Path.of("/data/AnkiDeckBuilder"),
            resolveApplicationDataDirectory(home, "Linux", null, "/data"),
        )
    }

    @Test
    fun linuxFallsBackWhenXdgDataHomeIsMissingOrBlank() {
        for (value in listOf(null, "", "  ")) {
            assertEquals(
                home.resolve(".local/share/AnkiDeckBuilder"),
                resolveApplicationDataDirectory(home, "Linux", null, value),
            )
        }
    }
}
