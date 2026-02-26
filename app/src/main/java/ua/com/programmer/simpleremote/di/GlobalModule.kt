package ua.com.programmer.simpleremote.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import ua.com.programmer.simpleremote.dao.database.AppDatabase
import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.ui.shared.FileManager
import ua.com.programmer.simpleremote.ui.shared.ImageLoader

@Module
@InstallIn(SingletonComponent::class)
class GlobalModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideConnectionSettingsDao(appDatabase: AppDatabase): ConnectionSettingsDao {
        return appDatabase.connectionSettingsDao()
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