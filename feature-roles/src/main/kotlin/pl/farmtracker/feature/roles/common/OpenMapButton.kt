package pl.farmtracker.feature.roles.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pl.farmtracker.core.ui.component.BigActionButton
import pl.farmtracker.core.ui.component.Tone
import pl.farmtracker.feature.roles.R

/** Wspólny dla ról przycisk „Mapa" – zawsze na końcu ekranu, pod głównymi akcjami. */
@Composable
internal fun OpenMapButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    BigActionButton(
        text = stringResource(R.string.roles_open_map),
        icon = Icons.Filled.Map,
        onClick = onClick,
        tone = Tone.Primary,
        modifier = modifier,
    )
}
