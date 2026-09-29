package pl.farmtracker.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.farmtracker.data.field.DataStoreFieldRepository
import pl.farmtracker.data.field.FieldRepository
import pl.farmtracker.data.network.HttpGet
import pl.farmtracker.data.network.UrlConnectionHttpGet
import pl.farmtracker.data.parcel.ParcelRepository
import pl.farmtracker.data.parcel.UldkParcelRepository
import pl.farmtracker.data.place.NominatimPlaceRepository
import pl.farmtracker.data.place.PlaceRepository
import pl.farmtracker.data.session.DataStoreSessionRepository
import pl.farmtracker.data.session.SessionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {

    @Binds
    fun bindSessionRepository(impl: DataStoreSessionRepository): SessionRepository

    @Binds
    fun bindParcelRepository(impl: UldkParcelRepository): ParcelRepository

    @Binds
    fun bindFieldRepository(impl: DataStoreFieldRepository): FieldRepository

    @Binds
    fun bindPlaceRepository(impl: NominatimPlaceRepository): PlaceRepository

    @Binds
    fun bindHttpGet(impl: UrlConnectionHttpGet): HttpGet

    companion object {
        @Provides
        @Singleton
        fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("session") },
            )
    }
}
