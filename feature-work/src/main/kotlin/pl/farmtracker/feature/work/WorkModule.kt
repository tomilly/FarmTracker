package pl.farmtracker.feature.work

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WorkModule {

    @Binds
    fun bindLocationSource(impl: FusedLocationSource): LocationSource
}
