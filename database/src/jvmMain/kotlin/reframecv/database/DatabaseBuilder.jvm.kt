package reframecv.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

fun createDatabaseBuilder(databaseFile: Path): RoomDatabase.Builder<AppDatabase> {
    databaseFile.parent?.let(Files::createDirectories)
    return Room.databaseBuilder<AppDatabase>(name = databaseFile.absolutePathString())
}
