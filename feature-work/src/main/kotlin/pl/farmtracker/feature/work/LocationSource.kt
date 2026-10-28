package pl.farmtracker.feature.work

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import pl.farmtracker.core.domain.GpsPace
import pl.farmtracker.core.domain.geo.GeoPoint
import javax.inject.Inject

/** Odczyty GPS tego telefonu – tak często, jak każe [GpsPace] (zmiana tempa przestawia GPS od razu). */
fun interface LocationSource {
    fun updates(pace: Flow<GpsPace>): Flow<GeoPoint>
}

/**
 * Pozycja z usług Google (GPS + sieć). W ruchu co 5 s – częściej nie ma sensu, bo i tak wysyłamy rzadziej
 * ([pl.farmtracker.core.domain.PositionReport.needsUpdate]); na postoju co 30 s – oszczędza baterię.
 */
class FusedLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun updates(pace: Flow<GpsPace>): Flow<GeoPoint> =
        pace.distinctUntilChanged().flatMapLatest { updatesEvery(it.intervalMillis) }

    // WorkService startuje tylko ze zgodą na lokalizację.
    @SuppressLint("MissingPermission")
    private fun updatesEvery(intervalMillis: Long): Flow<GeoPoint> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        // Dokładność zawsze wysoka – na postoju tylko rzadziej (zgrubna pozycja z sieci wskazałaby złe pole).
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MILLIS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                // Pozycja „z grubsza" (np. tylko z sieci komórkowej) wskazałaby złe pole.
                if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_METERS) return
                trySend(GeoPoint(latitude = location.latitude, longitude = location.longitude))
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val MIN_INTERVAL_MILLIS = 2_000L
        const val MAX_ACCURACY_METERS = 75f
    }
}
