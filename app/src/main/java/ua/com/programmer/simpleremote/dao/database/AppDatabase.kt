package ua.com.programmer.simpleremote.dao.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.com.programmer.simpleremote.dao.entity.CachedContentLine
import ua.com.programmer.simpleremote.dao.entity.CachedDocument
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@Database(
    entities = [ConnectionSettings::class, CachedDocument::class, CachedContentLine::class],
    version = 7
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun connectionSettingsDao(): ConnectionSettingsDao
    abstract fun documentCacheDao(): DocumentCacheDao
}