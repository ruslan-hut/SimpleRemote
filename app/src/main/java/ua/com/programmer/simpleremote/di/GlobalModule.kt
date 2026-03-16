package ua.com.programmer.simpleremote.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.gson.Gson
import ua.com.programmer.simpleremote.dao.database.AppDatabase
import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.dao.database.DocumentCacheDao
import ua.com.programmer.simpleremote.ui.shared.FileManager
import ua.com.programmer.simpleremote.ui.shared.ImageLoader

@Module
@InstallIn(SingletonComponent::class)
class GlobalModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "simple_remote_database"
        )
            .addMigrations(MIGRATION_3_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()
    }

    companion object {
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_documents ADD COLUMN document_title TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE cached_documents (" +
                            "id TEXT NOT NULL PRIMARY KEY," +
                            "connection_guid TEXT NOT NULL DEFAULT ''," +
                            "document_guid TEXT NOT NULL DEFAULT ''," +
                            "document_type TEXT NOT NULL DEFAULT ''," +
                            "document_json TEXT NOT NULL DEFAULT ''," +
                            "created_at INTEGER NOT NULL DEFAULT 0," +
                            "updated_at INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "CREATE TABLE cached_content_lines (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "cache_id TEXT NOT NULL DEFAULT ''," +
                            "line INTEGER NOT NULL DEFAULT 0," +
                            "content_json TEXT NOT NULL DEFAULT ''," +
                            "FOREIGN KEY (cache_id) REFERENCES cached_documents(id) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX index_cached_content_lines_cache_id_line ON cached_content_lines(cache_id, line)"
                )
            }
        }

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

                db.execSQL("INSERT INTO connection_settings (" +
                        "description, server_address, database_name, user, password, guid" +
                        ") SELECT " +
                        "alias, server_address, database_name, user_name, user_password, LOWER(HEX(RANDOMBLOB(16))" +
                        ") FROM connections")

                db.execSQL("DROP TABLE connections")

                db.execSQL("UPDATE connection_settings SET is_current = 1 WHERE guid = (SELECT MIN(guid) FROM connection_settings)")
            }
        }
    }

    @Provides
    @Singleton
    fun provideConnectionSettingsDao(appDatabase: AppDatabase): ConnectionSettingsDao {
        return appDatabase.connectionSettingsDao()
    }

    @Provides
    @Singleton
    fun provideDocumentCacheDao(appDatabase: AppDatabase): DocumentCacheDao {
        return appDatabase.documentCacheDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader(context)
    }

    @Provides
    @Singleton
    fun provideFileManager(@ApplicationContext context: Context): FileManager {
        return FileManager(context)
    }

}