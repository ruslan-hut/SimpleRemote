package ua.com.programmer.simpleremote.dao.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings

@Database(entities = [ConnectionSettings::class], version = 5)

abstract class AppDatabase: RoomDatabase() {

    abstract fun connectionSettingsDao(): ConnectionSettingsDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE connection_settings (" +
                        "guid TEXT NOT NULL DEFAULT ''," +
                        "server_address TEXT NOT NULL DEFAULT ''," +
                        "description TEXT NOT NULL DEFAULT ''," +
                        "database_name TEXT NOT NULL DEFAULT ''," +
                        "user TEXT NOT NULL DEFAULT ''," +
                        "password TEXT NOT NULL DEFAULT ''," +
                        "user_options TEXT NOT NULL DEFAULT ''," +
                        "is_current INTEGER NOT NULL DEFAULT 0," +
                        "auto_connect INTEGER NOT NULL DEFAULT 0," +
                        "PRIMARY KEY(guid))")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simple_remote_database")
                    .addMigrations(
                        MIGRATION_3_5
                    )
                    .build()
                INSTANCE = instance
                return instance
            }
        }

    }
}