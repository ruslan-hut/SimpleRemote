package ua.com.programmer.simpleremote.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ua.com.programmer.simpleremote.repository.DataLoader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Repository {

    @Provides
    @Singleton
    fun provideDataLoader(@ApplicationContext context: Context): DataLoader {
        return DataLoader(context)
    }

}