package ankideckbuilder.filesystem

import java.nio.file.Path

private const val APPLICATION_DIRECTORY_NAME = "AnkiDeckBuilder"

fun applicationDataDirectory(): Path = resolveApplicationDataDirectory(
    userHome = Path.of(requireNotNull(System.getProperty("user.home"))),
    operatingSystemName = System.getProperty("os.name"),
    appData = System.getenv("APPDATA"),
    xdgDataHome = System.getenv("XDG_DATA_HOME"),
)

internal fun resolveApplicationDataDirectory(
    userHome: Path,
    operatingSystemName: String,
    appData: String?,
    xdgDataHome: String?,
): Path {
    val operatingSystem = operatingSystemName.lowercase()

    return when {
        operatingSystem.contains("mac") ->
            userHome.resolve("Library/Application Support/$APPLICATION_DIRECTORY_NAME")

        operatingSystem.contains("win") ->
            appData
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
                ?.resolve(APPLICATION_DIRECTORY_NAME)
                ?: userHome.resolve("AppData/Roaming/$APPLICATION_DIRECTORY_NAME")

        else ->
            xdgDataHome
                ?.takeIf(String::isNotBlank)
                ?.let(Path::of)
                ?.resolve(APPLICATION_DIRECTORY_NAME)
                ?: userHome.resolve(".local/share/$APPLICATION_DIRECTORY_NAME")
    }
}
