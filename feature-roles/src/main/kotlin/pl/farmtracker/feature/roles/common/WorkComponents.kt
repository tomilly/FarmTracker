package pl.farmtracker.feature.roles.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R

private val LocationPermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * „Zaczynam pracę": najpierw zgoda na lokalizację (i na powiadomienie, które pokazuje, że jest włączona).
 * Bez zgody praca się nie zaczyna – pod przyciskiem wyjaśnienie i droga do ustawień.
 */
@Composable
internal fun StartWorkButton(onStartWork: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        denied = !context.hasLocationPermission()
        if (!denied) onStartWork()
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BigActionButton(
            text = stringResource(R.string.roles_start_work),
            icon = Icons.Filled.PlayArrow,
            onClick = {
                if (context.hasLocationPermission()) {
                    denied = false
                    onStartWork()
                } else {
                    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        listOf(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        emptyList()
                    }
                    launcher.launch((LocationPermissions + notifications).toTypedArray())
                }
            },
            tone = Tone.Go,
        )
        if (denied) {
            StatusPill(text = stringResource(R.string.roles_location_denied), icon = Icons.Filled.LocationOff, tone = Tone.Stop)
            BigActionButton(
                text = stringResource(R.string.roles_open_settings),
                icon = Icons.Filled.Settings,
                onClick = { context.openAppSettings() },
                tone = Tone.Neutral,
            )
        }
    }
}

@Composable
internal fun StopWorkButton(onStopWork: () -> Unit, modifier: Modifier = Modifier) {
    BigActionButton(
        text = stringResource(R.string.roles_stop_work),
        icon = Icons.Filled.Stop,
        onClick = onStopWork,
        modifier = modifier,
        tone = Tone.Stop,
    )
}

/** Zawsze widać, że inni widzą moją pozycję (CLAUDE.md) – i gdzie według telefonu jestem. */
@Composable
internal fun MyPositionPills(position: MyPosition, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(text = stringResource(R.string.roles_location_shared), icon = Icons.Filled.MyLocation, tone = Tone.Go)
        when (position) {
            MyPosition.Searching -> StatusPill(
                text = stringResource(R.string.roles_position_searching),
                icon = Icons.Filled.LocationSearching,
            )
            is MyPosition.OnField -> StatusPill(
                text = stringResource(R.string.roles_position_on_field, position.fieldName),
                icon = Icons.Filled.Agriculture,
                tone = Tone.Go,
            )
            MyPosition.OffField -> StatusPill(
                text = stringResource(R.string.roles_position_off_field),
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                tone = Tone.Warning,
            )
        }
    }
}

/** Gdzie są inni w danej roli, np. „Marek – na polu Za lasem"; nikt – „Sieczkarnia nie pracuje". */
@Composable
internal fun CoworkerPills(role: Role, coworkers: List<Coworker>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (coworkers.isEmpty()) {
            StatusPill(
                text = stringResource(R.string.roles_nobody_working, stringResource(RoleUi.labelRes(role))),
                icon = RoleUi.icon(role),
            )
        }
        coworkers.forEach { coworker ->
            val where = when {
                coworker.isStale -> stringResource(R.string.roles_coworker_stale)
                coworker.fieldName != null -> stringResource(R.string.roles_coworker_on_field, coworker.fieldName)
                else -> stringResource(R.string.roles_coworker_off_field)
            }
            StatusPill(
                text = stringResource(R.string.roles_coworker, coworker.name, where),
                icon = RoleUi.icon(coworker.role),
                tone = when {
                    coworker.isStale -> Tone.Neutral
                    coworker.fieldName != null -> Tone.Go
                    else -> Tone.Warning
                },
            )
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    LocationPermissions.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@PreviewLightDark
@Composable
private fun WorkPillsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MyPositionPills(MyPosition.OnField("Za lasem"))
                CoworkerPills(
                    role = Role.HARVESTER,
                    coworkers = listOf(
                        Coworker("Marek", Role.HARVESTER, fieldName = "Za lasem", isStale = false),
                        Coworker("Rysiek", Role.HARVESTER, fieldName = null, isStale = true),
                    ),
                )
            }
        }
    }
}
