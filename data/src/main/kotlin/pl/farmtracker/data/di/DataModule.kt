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
import pl.farmtracker.data.auth.AuthRepository
import pl.farmtracker.data.auth.FirebaseAuthRepository
import pl.farmtracker.data.harvest.FirestoreHarvestRepository
import pl.farmtracker.data.harvest.HarvestRepository
import pl.farmtracker.data.network.HttpGet
import pl.farmtracker.data.network.UrlConnectionHttpGet
import pl.farmtracker.data.parcel.ParcelRepository
import pl.farmtracker.data.parcel.UldkParcelRepository
import pl.farmtracker.data.place.NominatimPlaceRepository
import pl.farmtracker.data.place.PlaceRepository
import pl.farmtracker.data.session.DataStoreSessionRepository
import pl.farmtracker.data.session.SessionRepository
import pl.farmtracker.data.time.Clock
import pl.farmtracker.data.time.SystemClock
import pl.farmtracker.data.work.DataStoreWorkRepository
import pl.farmtracker.data.work.WorkRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {

    @Binds
    fun bindSessionRepository(impl: DataStoreSessionRepository): SessionRepository

    @Binds
    fun bindParcelRepository(impl: UldkParcelRepository): ParcelRepository

    // Pola, baza i pozycje: na telefonie albo w zbiorze (Firestore) – wybiera aplikacja (moduł app), bo zależy to od
    // tego, czy jest skonfigurowany Firebase.

    @Binds
    fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    fun bindHarvestRepository(impl: FirestoreHarvestRepository): HarvestRepository

    @Binds
    fun bindPlaceRepository(impl: NominatimPlaceRepository): PlaceRepository

    @Binds
    fun bindHttpGet(impl: UrlConnectionHttpGet): HttpGet

    @Binds
    fun bindWorkRepository(impl: DataStoreWorkRepository): WorkRepository

    @Binds
    fun bindClock(impl: SystemClock): Clock

    companion object {
        @Provides
        @Singleton
        fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("session") },
            )
    }
}
