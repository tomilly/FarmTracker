package pl.farmtracker.feature.roles.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.Trip
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.theme.FarmTrackerTheme
import pl.farmtracker.feature.roles.R

/**
 * Osoba w jednym wierszu, jak w aplikacjach flotowych: ikona roli, imię, co robi; żółta etykieta „stoi 12 min"
 * tylko wtedy, gdy stoi – to jedyne, na co trzeba zwrócić uwagę.
 */
@Composable
internal fun CoworkerRow(coworker: Coworker, modifier: Modifier = Modifier) {
    PersonRow(
        icon = RoleUi.icon(coworker.role),
        name = coworker.name,
        doing = coworker.doing(),
        faded = coworker.isStale,
        modifier = modifier,
    ) {
        coworker.standingMinutes?.let { StandingBadge(it) }
    }
}

/** Ktoś ze zbioru, kto nie zaczął pracy – szary, żeby nie mylił się z pracującymi. */
@Composable
internal fun NotWorkingRow(name: String, modifier: Modifier = Modifier) {
    PersonRow(
        icon = Icons.Filled.PersonOff,
        name = name,
        doing = stringResource(R.string.roles_coworker_not_working),
        faded = true,
        modifier = modifier,
    )
}

@Composable
private fun PersonRow(
    icon: ImageVector,
    name: String,
    doing: String,
    faded: Boolean,
    modifier: Modifier = Modifier,
    badge: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = if (faded) colors.outline else colors.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = if (faded) colors.outline else colors.onSurface)
            Text(
                doing,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        badge()
    }
}

@Composable
private fun StandingBadge(minutes: Int) {
    val semantic = FarmTrackerTheme.semanticColors
    Surface(shape = RoundedCornerShape(8.dp), color = semantic.warning, contentColor = semantic.onWarning) {
        Text(
            stringResource(R.string.roles_coworker_standing_badge, minutes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun CrewRowsPreview() {
    FarmTrackerTheme(darkTheme = isSystemInDarkTheme()) {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CoworkerRow(Coworker("Rysiek", Role.HARVESTER, fieldName = "Za lasem", isStale = false, standingMinutes = 7))
                CoworkerRow(Coworker("Marek", Role.DRIVER, fieldName = "Za lasem", isStale = false, trip = Trip.LOADING))
                CoworkerRow(
                    Coworker("Janek", Role.DRIVER, fieldName = null, isStale = false, trip = Trip.TO_FIELD, distanceMeters = 1_200.0),
                )
                CoworkerRow(Coworker("Wojtek", Role.DRIVER, fieldName = null, isStale = true))
                NotWorkingRow("Staszek")
            }
        }
    }
}
