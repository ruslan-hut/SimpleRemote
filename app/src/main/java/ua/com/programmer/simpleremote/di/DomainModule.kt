package ua.com.programmer.simpleremote.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import ua.com.programmer.simpleremote.dao.database.AppDatabase
import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.dao.impl.ConnectionSettingsImpl
import ua.com.programmer.simpleremote.http.NetworkRepositoryImpl
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import ua.com.programmer.simpleremote.repository.NetworkRepository

@Module
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
class DomainModule {
    @Provides
    fun provideConnectionSettingsDao(db: AppDatabase): ConnectionSettingsDao {
        return db.connectionSettingsDao()
    }
}

@Module
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
abstract class RepositoryBindModule {
    @Binds
    abstract fun bindConnectionSettingsRepository(repositoryImpl: ConnectionSettingsImpl): ConnectionSettingsRepository

    @Binds
    abstract fun bindNetworkRepository(repositoryImpl: NetworkRepositoryImpl): NetworkRepository
}