package pl.farmtracker.feature.roles.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.activeFieldIds
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapScaffold
import pl.farmtracker.core.map.toMapPeople
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.component.SwitchRoleButton

/**
 * Mapa w czasie pracy (kierowca, sieczkarnia): pola z podświetlonym polem sieczkarni, wjazdy, baza, inni pracujący.
 * Mapa jedzie za mną i trzyma na ekranie tych, których [keepInView] wybierze – do nich jadę albo oni do mnie.
 */
internal fun CrewSnapshot.toWorkMapOverlays(base: GeoPoint?, keepInView: (LiveLocation) -> Boolean): MapOverlays =
    MapOverlays(
        fields = fields,
        entryPoints = fields.flatMap { it.entryPoints },
        base = base,
        people = locations.toMapPeople(nowMillis),
        activeFieldIds = locations.activeFieldIds(nowMillis),
        // Stara pozycja (brak zasięgu) nie oddala mapy – mogła się już dawno zmienić.
        keepInView = locations.filter { !it.isMe && !it.isStaleAt(nowMillis) && keepInView(it) }.map { it.point },
    )

/** Praca bez klikania: mapa na cały ekran, a pod nią [content] – gdzie jestem, gdzie inni, mały „Kończę pracę". */
@Composable
internal fun WorkMap(
    role: Role,
    chrome: MapChromeController,
    overlays: MapOverlays,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    MapScaffold(
        title = stringResource(RoleUi.labelRes(role)),
        icon = RoleUi.icon(role),
        onBack = null,
        chrome = chrome,
        onMapTap = {},
        modifier = modifier,
        overlays = overlays,
        showParcelsToggle = false,
        actions = { if (onSwitchRole != null) SwitchRoleButton(onClick = onSwitchRole) },
        panel = content,
    )
}
