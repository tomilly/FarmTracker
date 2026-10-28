package pl.farmtracker.feature.roles.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import pl.farmtracker.core.domain.DriveTo
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R

/**
 * „Prowadź do pola: Za lasem" / „Prowadź do bazy" – nawigacja w Google Maps jednym dotknięciem; cel wybiera telefon
 * ([pl.farmtracker.core.domain.driveTo]), kierowca niczego nie wpisuje. Bez celu – bez przycisku.
 */
@Composable
internal fun DriveToButton(driveTo: DriveTo?, modifier: Modifier = Modifier) {
    if (driveTo == null) return
    val context = LocalContext.current
    BigActionButton(
        text = when (driveTo) {
            is DriveTo.ToField -> stringResource(R.string.roles_drive_to_field, driveTo.fieldName)
            is DriveTo.ToBase -> stringResource(R.string.roles_drive_to_base)
        },
        icon = Icons.Filled.Navigation,
        onClick = { context.navigateTo(driveTo.point) },
        tone = Tone.Primary,
        modifier = modifier,
    )
}

/** Nawigacja samochodowa w Google Maps; bez Google Maps – dowolna aplikacja z mapą (punkt docelowy). */
private fun Context.navigateTo(point: GeoPoint) {
    val target = "${point.latitude},${point.longitude}"
    val googleMaps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$target&mode=d"))
        .setPackage(GOOGLE_MAPS_PACKAGE)
    try {
        startActivity(googleMaps)
    } catch (error: ActivityNotFoundException) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$target?q=$target")))
        } catch (noMapApp: ActivityNotFoundException) {
            // Telefon bez żadnej mapy – przycisk nic nie zrobi, aplikacja się nie wywróci.
        }
    }
}

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

@PreviewLightDark
@Composable
private fun DriveToButtonPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Column {
            DriveToButton(DriveTo.ToField("Za lasem", GeoPoint(50.0, 17.0)))
            DriveToButton(DriveTo.ToBase(GeoPoint(50.0, 17.0)))
        }
    }
}
