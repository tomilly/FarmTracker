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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.StatusPill
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.format.formatDistance
import pl.farmtracker.core.ui.theme.FarmTrackerDimens
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
                // Pyta tylko o brakujące. Odmowa powiadomienia nie blokuje pracy – ekran roli i tak pokazuje,
                // że lokalizacja jest włączona; bez zgody na lokalizację praca się nie zaczyna.
                val missing = context.missingWorkPermissions()
                if (missing.isEmpty()) {
                    denied = false
                    onStartWork()
                } else {
                    launcher.launch(missing.toTypedArray())
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

/**
 * „Kończę" – celowo mały kwadrat w lewym dolnym rogu, z dala od „Prowadź do…", i z potwierdzeniem: przypadkowe
 * zakończenie wyłącza lokalizację, a inni tracą maszynę z mapy.
 */
@Composable
internal fun StopWorkButton(onStopWork: () -> Unit, modifier: Modifier = Modifier) {
    var confirming by rememberSaveable { mutableStateOf(false) }
    val error = MaterialTheme.colorScheme.error
    OutlinedButton(
        onClick = { confirming = true },
        modifier = modifier.widthIn(min = FarmTrackerDimens.BigButtonMinHeight).heightIn(min = FarmTrackerDimens.BigButtonMinHeight),
        shape = MaterialTheme.shapes.medium,
        // Nieprzezroczysty – czytelny także na mapie.
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = error,
        ),
        border = BorderStroke(2.dp, error),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Text(stringResource(R.string.roles_stop_work_short), style = MaterialTheme.typography.labelMedium)
        }
    }
    if (confirming) {
        StopWorkDialog(
            onConfirm = {
                confirming = false
                onStopWork()
            },
            onDismiss = { confirming = false },
        )
    }
}

/** „Skończyć pracę?" – po małym przycisku na ekranie roli i po „Kończę pracę" w powiadomieniu. */
@Composable
fun StopWorkDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.roles_stop_work_question)) },
        text = { Text(stringResource(R.string.roles_stop_work_explanation), style = MaterialTheme.typography.bodyLarge) },
        // Oba przyciski na całą szerokość, jeden pod drugim – łatwo trafić w rękawicach.
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.roles_stop_work_confirm), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().heightIn(min = FarmTrackerDimens.MinTouchTarget),
                ) {
                    Text(stringResource(R.string.roles_stop_work_cancel), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
    )
}

/** Stan jednym słowem w karcie na mapie: „Ładujesz", „Na polu Za lasem"… */
@Composable
internal fun MyPosition.title(): String = when (this) {
    MyPosition.Searching -> stringResource(R.string.roles_position_searching)
    is MyPosition.OnField -> stringResource(R.string.roles_position_on_field, fieldName)
    MyPosition.OffField -> stringResource(R.string.roles_position_off_field)
    MyPosition.Loading -> stringResource(R.string.roles_position_loading)
    MyPosition.ToBase -> stringResource(R.string.roles_position_to_base)
    MyPosition.ToField -> stringResource(R.string.roles_position_to_field)
    MyPosition.AtBase -> stringResource(R.string.roles_position_at_base)
}

internal val MyPosition.icon: ImageVector
    get() = when (this) {
        MyPosition.Searching -> Icons.Filled.LocationSearching
        is MyPosition.OnField -> Icons.Filled.Grass
        MyPosition.OffField -> Icons.AutoMirrored.Filled.ArrowForward
        MyPosition.Loading -> Icons.Filled.Download
        MyPosition.ToBase -> Icons.Filled.LocalShipping
        MyPosition.ToField -> Icons.Filled.Route
        MyPosition.AtBase -> Icons.Filled.Warehouse
    }

/** Co robi kierowca – jego status wykryty z pozycji, np. „ładuje", „wraca do bazy". */
@Composable
private fun Coworker.tripText(): String? = when (trip) {
    Trip.LOADING -> stringResource(R.string.roles_coworker_loading)
    Trip.TO_BASE -> stringResource(R.string.roles_coworker_to_base)
    Trip.TO_FIELD -> stringResource(R.string.roles_coworker_to_field)
    Trip.AT_BASE -> stringResource(R.string.roles_coworker_at_base)
    Trip.ON_FIELD, null -> null
}

/**
 * Co robi ktoś inny: „ładuje", „na polu Za lasem", „jedzie na pole · 1,2 km" – odległość tylko, gdy jest w drodze
 * (wtedy ważne, kiedy dojedzie): jedzie na pole / do bazy albo jest poza polami.
 */
@Composable
internal fun Coworker.doing(): String {
    val tripText = tripText()
    val what = when {
        isStale -> stringResource(R.string.roles_coworker_stale)
        tripText != null -> tripText
        fieldName != null -> stringResource(R.string.roles_coworker_on_field, fieldName)
        else -> stringResource(R.string.roles_coworker_off_field)
    }
    val onTheRoad = trip in ON_THE_ROAD || (fieldName == null && trip != Trip.AT_BASE)
    val distance = distanceMeters?.takeIf { !isStale && onTheRoad } ?: return what
    return stringResource(R.string.roles_coworker_distance, what, formatDistance(distance))
}

private val ON_THE_ROAD = setOf(Trip.TO_FIELD, Trip.TO_BASE)

/**
 * Linijki do karty stanu: „Rysiek – na polu Za lasem, stoi 7 min", „Marek – ładuje"; nikt – „Sieczkarnia nie
 * pracuje".
 */
@Composable
internal fun crewLines(role: Role, coworkers: List<Coworker>): List<String> {
    if (coworkers.isEmpty()) {
        return listOf(stringResource(R.string.roles_nobody_working, stringResource(RoleUi.labelRes(role))))
    }
    return coworkers.map { coworker ->
        val doing = coworker.doing()
        val status = coworker.standingMinutes?.let { stringResource(R.string.roles_coworker_standing, doing, it) } ?: doing
        stringResource(R.string.roles_coworker, coworker.name, status)
    }
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.hasLocationPermission(): Boolean = LocationPermissions.any { isGranted(it) }

/** Lokalizacja (gdy nie ma żadnej) i powiadomienie o pracy (Android 13+), o które jeszcze nie zapytano skutecznie. */
private fun Context.missingWorkPermissions(): List<String> {
    val location = if (hasLocationPermission()) emptyList() else LocationPermissions
    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !isGranted(Manifest.permission.POST_NOTIFICATIONS)
    ) {
        listOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyList()
    }
    return location + notifications
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@PreviewLightDark
@Composable
private fun StopWorkDialogPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        StopWorkDialog(onConfirm = {}, onDismiss = {})
    }
}

@PreviewLightDark
@Composable
private fun StopWorkButtonPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Box(Modifier.padding(16.dp)) { StopWorkButton(onStopWork = {}) }
        }
    }
}
