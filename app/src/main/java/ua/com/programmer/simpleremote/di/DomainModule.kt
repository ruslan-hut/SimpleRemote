package ua.com.programmer.simpleremote.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import ua.com.programmer.simpleremote.http.impl.NetworkRepositoryImpl
import ua.com.programmer.simpleremote.repository.NetworkRepository

@Module
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
abstract class RepositoryBindModule {

    @Binds
    abstract fun bindNetworkRepository(repositoryImpl: NetworkRepositoryImpl): NetworkRepository
}