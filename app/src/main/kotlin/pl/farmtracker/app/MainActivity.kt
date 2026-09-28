package pl.farmtracker.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import pl.farmtracker.app.navigation.FarmTrackerApp
import pl.farmtracker.core.ui.theme.FarmTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Górny pasek jest zawsze ciemnozielony, więc ikony paska stanu zawsze jasne.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            FarmTrackerTheme {
                FarmTrackerApp()
            }
        }
    }
}
