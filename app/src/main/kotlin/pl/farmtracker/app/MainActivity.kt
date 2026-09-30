package pl.farmtracker.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import pl.farmtracker.app.navigation.AppViewModel
import pl.farmtracker.app.navigation.FarmTrackerApp
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.data.work.WorkRepository
import pl.farmtracker.feature.work.WorkService
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workRepository: WorkRepository

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Górny pasek jest zawsze ciemnozielony, więc ikony paska stanu zawsze jasne.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        // Po obrocie ekranu pytanie „Skończyć pracę?" pamięta ViewModel – nie pytamy drugi raz.
        if (savedInstanceState == null) handleWorkIntent(intent)
        setContent {
            FarmTrackerTheme {
                FarmTrackerApp(appViewModel)
            }
        }
        // „Zaczynam pracę" (także sprzed zamknięcia aplikacji) włącza udostępnianie lokalizacji. Tylko gdy ekran
        // jest widoczny – wtedy Android na to pozwala; wyłącza się samo, gdy praca się skończy.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                workRepository.isWorking.collect { working -> if (working) WorkService.start(this@MainActivity) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWorkIntent(intent)
    }

    /** „Kończę pracę" w powiadomieniu otwiera aplikację z pytaniem, czy na pewno. */
    private fun handleWorkIntent(intent: Intent?) {
        // Otwarcie z „ostatnich aplikacji" powtarza intencję, od której aplikacja wystartowała – wtedy nie pytamy.
        val fromRecents = intent != null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (intent?.action == WorkService.ACTION_ASK_STOP && !fromRecents) appViewModel.askToStopWork()
    }
}
