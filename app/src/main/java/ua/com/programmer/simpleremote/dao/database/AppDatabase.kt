package ua.com.programmer.simpleremote.dao.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@Database(entities = [ConnectionSettings::class], version = 5)
abstract class AppDatabase: RoomDatabase() {
    abstract fun connectionSettingsDao(): ConnectionSettingsDao
}