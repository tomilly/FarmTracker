package pl.farmtracker.feature.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.farmtracker.core.domain.Field
import pl.farmtracker.data.location.LiveLocationRepository
import pl.farmtracker.data.work.WorkRepository
import javax.inject.Inject

/**
 * Udostępnianie lokalizacji od „Zaczynam pracę" do „Kończę pracę" (CLAUDE.md): działa też przy zgaszonym
 * ekranie, a stałe powiadomienie pokazuje, że lokalizacja jest włączona, i pozwala skończyć pracę.
 * Kończy się samo, gdy [WorkRepository.isWorking] zmieni się na `false` – z ekranu roli albo z powiadomienia.
 */
@AndroidEntryPoint
class WorkService : Service() {

    @Inject lateinit var workRepository: WorkRepository

    @Inject lateinit var liveLocationRepository: LiveLocationRepository

    @Inject lateinit var locationSource: LocationSource

    @Inject lateinit var publisher: LocationPublisher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sharing: Job? = null
    private var shownText: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch { workRepository.setWorking(false) }
            return START_NOT_STICKY
        }
        if (!hasLocationPermission() || !startInForeground()) {
            // Zgodę cofnięto w ustawieniach albo Android nie pozwolił (aplikacja w tle) – praca się kończy,
            // żeby ekran roli nie twierdził, że inni nas widzą.
            scope.launch { workRepository.setWorking(false) }
            stopSelf()
            return START_NOT_STICKY
        }
        if (sharing == null) sharing = scope.launch { share() }
        return START_STICKY
    }

    private suspend fun share() {
        val publishing = scope.launch { publisher.publish(locationSource.updates(), ::showField) }
        workRepository.isWorking.first { !it }
        publishing.cancel()
        liveLocationRepository.stopSharing()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(): Boolean = try {
        createChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(getString(R.string.work_notification_searching)), type)
        true
    } catch (error: RuntimeException) {
        // SecurityException / ForegroundServiceStartNotAllowedException – np. wznowienie przez system w tle.
        Log.w(TAG, "Nie włączono udostępniania lokalizacji", error)
        false
    }

    private fun showField(field: Field?) {
        val text = field?.let { getString(R.string.work_notification_on_field, it.name) }
            ?: getString(R.string.work_notification_off_field)
        if (text == shownText) return
        shownText = text
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canNotify) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private fun notification(text: String): Notification {
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
        }
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, WorkService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_work_notification)
            .setContentTitle(getString(R.string.work_notification_title))
            .setContentText(text)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.work_notification_stop), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.work_channel_name), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "FarmTrackerWork"
        private const val CHANNEL_ID = "work"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "pl.farmtracker.work.STOP"

        /**
         * Włącza udostępnianie, gdy trwa praca. Wołać z ekranu na wierzchu – Android pozwala zacząć śledzenie
         * lokalizacji tylko wtedy (ponowne wywołanie przy działającej usłudze nic nie psuje).
         */
        fun start(context: Context) {
            if (!context.hasLocationPermission()) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, WorkService::class.java))
            } catch (error: IllegalStateException) {
                Log.w(TAG, "Nie uruchomiono udostępniania lokalizacji", error)
            }
        }
    }
}

internal fun Context.hasLocationPermission(): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
