package pl.farmtracker.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.farmtracker.app.BuildConfig
import pl.farmtracker.data.base.BaseRepository
import pl.farmtracker.data.base.DataStoreBaseRepository
import pl.farmtracker.data.base.FirestoreBaseRepository
import pl.farmtracker.data.field.DataStoreFieldRepository
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.field.FirestoreFieldRepository
import pl.farmtracker.data.location.FirestoreLiveLocationRepository
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.location.LocalLiveLocationRepository
import pl.farmtracker.data.time.Clock
import pl.farmtracker.app.demo.DemoLiveLocationRepository
import javax.inject.Singleton
import javax.inject.Provider
import javax.inject.Qualifier

/**
 * `true`: jest Firebase – logowanie numerem telefonu, wspólny zbiór, pola i baza w Firestore.
 * `false` (brak `google-services.json`): jak przed M3 – wybór roli i dane tylko na telefonie.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SharedHarvest

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @SharedHarvest
    fun provideSharedHarvest(): Boolean = BuildConfig.SHARED_HARVEST

    @Provides
    fun provideFieldRepository(
        @SharedHarvest shared: Boolean,
        firestore: Provider<FirestoreFieldRepository>,
        local: Provider<DataStoreFieldRepository>,
    ): FieldRepository = if (shared) firestore.get() else local.get()

    @Provides
    fun provideBaseRepository(
        @SharedHarvest shared: Boolean,
        firestore: Provider<FirestoreBaseRepository>,
        local: Provider<DataStoreBaseRepository>,
    ): BaseRepository = if (shared) firestore.get() else local.get()

    @Provides
    @Singleton
    fun provideLiveLocationRepository(
        @SharedHarvest shared: Boolean,
        firestore: Provider<FirestoreLiveLocationRepository>,
        local: Provider<LocalLiveLocationRepository>,
        fields: Provider<FieldRepository>,
        base: Provider<BaseRepository>,
        clock: Clock,
    ): LiveLocationRepository = when {
        shared -> firestore.get()
        BuildConfig.DEMO -> DemoLiveLocationRepository(local.get(), fields.get(), base.get(), clock)
        else -> local.get()
    }
}
