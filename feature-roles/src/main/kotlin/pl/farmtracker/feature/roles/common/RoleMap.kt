package pl.farmtracker.feature.roles.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import pl.farmtracker.core.domain.Field
import pl.farmtracker.core.domain.LiveLocation
import pl.farmtracker.core.domain.Role
import pl.farmtracker.core.domain.activeFieldIds
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.GeoPolygon
import pl.farmtracker.core.map.FullScreenMap
import pl.farmtracker.core.map.MapChromeController
import pl.farmtracker.core.map.MapOverlays
import pl.farmtracker.core.map.MapTextButton
import pl.farmtracker.core.map.toMapPeople
import pl.farmtracker.core.ui.RoleUi
import pl.farmtracker.core.ui.R as CoreUiR

/**
 * Mapa ekranu roli: pola z podświetlonym polem sieczkarni, wjazdy, baza, inni pracujący. W pracy mapa jedzie za mną
 * i trzyma na ekranie tych, których [keepInView] wybierze – do nich jadę albo oni do mnie.
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

/**
 * Każdy ekran roli to mapa na cały ekran (bez górnego paska): karta stanu w rogu, najwyżej jeden duży przycisk
 * na dole albo arkusz z listą. „Admin" (admin, który sam pracuje – gdy nie pracuje) albo „Zmień rolę" (debug) –
 * małe, po prawej nad celownikiem.
 */
@Composable
internal fun RoleMap(
    chrome: MapChromeController,
    overlays: MapOverlays,
    onSwitchRole: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onBackToAdmin: (() -> Unit)? = null,
    status: @Composable () -> Unit = {},
    mapButtons: @Composable ColumnScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit = {},
    sheet: (@Composable ColumnScope.() -> Unit)? = null,
) {
    FullScreenMap(
        chrome = chrome,
        overlays = overlays,
        modifier = modifier,
        status = status,
        mapButtons = {
            mapButtons()
            when {
                onBackToAdmin != null -> MapTextButton(
                    text = stringResource(RoleUi.labelRes(Role.ADMIN)),
                    icon = RoleUi.icon(Role.ADMIN),
                    onClick = onBackToAdmin,
                )
                onSwitchRole != null -> MapTextButton(
                    text = stringResource(CoreUiR.string.core_ui_switch_role),
                    icon = Icons.Filled.SwapHoriz,
                    onClick = onSwitchRole,
                )
            }
        },
        bottom = bottom,
        sheet = sheet,
    )
}

/** Cały zbiór na jednym widoku – wszystkie pola i baza (admin, baza). Bez pól zostaje zwykły widok (ja). */
internal suspend fun MapChromeController.showWholeHarvest(fields: Flow<List<Field>>, base: Flow<GeoPoint?>) {
    val shapes = withTimeoutOrNull(FIELDS_WAIT_MILLIS) { fields.first { it.isNotEmpty() } }
        ?.flatMap { it.shape }
        ?: return
    val basePoint = base.first()
    showArea(shapes + listOfNotNull(basePoint?.let { GeoPolygon(listOf(it)) }))
}

/** Pola z Firestore przychodzą po chwili; dłużej nie czekamy – mapa nie może potem nagle skoczyć. */
private const val FIELDS_WAIT_MILLIS = 5_000L
