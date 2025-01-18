package ua.com.programmer.simpleremote.dao.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@Database(entities = [ConnectionSettings::class], version = 5)

abstract class AppDatabase: RoomDatabase() {

    abstract fun connectionSettingsDao(): ConnectionSettingsDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_remote_database")
                    .build()
                INSTANCE = instance
                return instance
            }
        }

    }
}