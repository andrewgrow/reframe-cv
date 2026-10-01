package reframecv.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import reframecv.filesystem.applicationDataDirectory

private const val DATABASE_FILE_NAME = "reframe-cv.db"

fun createDatabaseBuilder(databaseFile: Path): RoomDatabase.Builder<AppDatabase> {
    databaseFile.parent?.let(Files::createDirectories)
    return Room.databaseBuilder<AppDatabase>(name = databaseFile.absolutePathString())
}

fun openDefaultDatabase(): AppDatabase = buildDatabase(createDatabaseBuilder(defaultDatabaseFile()))

fun defaultDatabaseFile(): Path = applicationDataDirectory().resolve(DATABASE_FILE_NAME)
