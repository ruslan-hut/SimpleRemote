package ua.com.programmer.simpleremote.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import ua.com.programmer.simpleremote.BuildConfig
import ua.com.programmer.simpleremote.dao.database.AppDatabase
import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.dao.impl.ConnectionSettingsImpl
import ua.com.programmer.simpleremote.http.client.HttpAuthInterceptor
import ua.com.programmer.simpleremote.http.client.TokenRefresh
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class Repository {

    @Provides
    @Singleton
    fun provideConnectionRepository(dao: ConnectionSettingsDao, database: AppDatabase): ConnectionSettingsRepository {
        return ConnectionSettingsImpl(dao, database)
    }

}

@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

    @Provides
    @Singleton
    fun provideInterceptor(): HttpAuthInterceptor {
        return HttpAuthInterceptor()
    }

    @Provides
    @Singleton
    fun provideAuthenticator(): TokenRefresh {
        return TokenRefresh()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: HttpAuthInterceptor, authenticator: TokenRefresh): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(authenticator)
            .build()
    }

}