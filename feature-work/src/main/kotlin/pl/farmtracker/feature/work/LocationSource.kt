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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.farmtracker.core.domain.geo.GeoPoint
import javax.inject.Inject

/** Odczyty GPS tego telefonu. */
fun interface LocationSource {
    fun updates(): Flow<GeoPoint>
}

/**
 * Pozycja z usług Google (GPS + sieć). Co [INTERVAL_MILLIS] – częściej nie ma sensu, bo i tak wysyłamy
 * rzadziej ([pl.farmtracker.core.domain.PositionReport.needsUpdate]).
 */
class FusedLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationSource {

    // WorkService startuje tylko ze zgodą na lokalizację.
    @SuppressLint("MissingPermission")
    override fun updates(): Flow<GeoPoint> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MILLIS)
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
        const val INTERVAL_MILLIS = 5_000L
        const val MIN_INTERVAL_MILLIS = 2_000L
        const val MAX_ACCURACY_METERS = 75f
    }
}
