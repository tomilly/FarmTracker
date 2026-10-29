package pl.farmtracker.core.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.farmtracker.core.domain.geo.GeoPoint

private val EdgePadding = 12.dp
private val SheetMaxHeightFraction = 0.55f

/**
 * Ekran roli jako mapa na cały ekran – bez górnego paska (więcej mapy, jak w nawigacji). Na mapie:
 * w lewym górnym rogu [status] (mała karta stanu), po prawej okrągłe ikony – warstwy u góry, celownik „Gdzie jestem"
 * nad dolną częścią; na dole [bottom] (najwyżej jeden duży przycisk) i/lub [sheet] – zaokrąglony arkusz na całą
 * szerokość (lista ludzi, menu), który zostawia mapę widoczną. Kamera wie, ile mapy zasłaniają, więc pozycja i pola
 * trafiają w odkrytą część.
 *
 * @param mapButtons dodatkowe przyciski nad celownikiem (np. „Duża mapa", „Admin") – nie u góry, żeby nie zwężały karty
 * stanu
 */
@Composable
fun FullScreenMap(
    chrome: MapChromeController,
    overlays: MapOverlays,
    modifier: Modifier = Modifier,
    onMapTap: (GeoPoint) -> Unit = {},
    status: @Composable () -> Unit = {},
    mapButtons: @Composable ColumnScope.() -> Unit = {},
    bottom: @Composable ColumnScope.() -> Unit = {},
    sheet: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val chromeState by chrome.state.collectAsStateWithLifecycle()
    LocationPermissionEffects(chrome, chromeState)

    var coveredTop by remember { mutableIntStateOf(0) }
    var coveredBottom by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val sheetMaxHeight = maxHeight * SheetMaxHeightFraction
        FarmMap(
            chrome = chromeState,
            overlays = overlays,
            covered = MapCovered(top = coveredTop, bottom = coveredBottom),
            onCameraIdle = chrome::onCameraIdle,
            onMapTap = onMapTap,
            onCameraRequestHandled = chrome::onCameraRequestHandled,
            modifier = Modifier.fillMaxSize(),
            onFollowingChanged = chrome::onFollowingChanged,
        )
        // Ikony paska systemu są jasne – na jasnej mapie potrzebują ciemniejszego tła.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(StatusBarScrim),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { coveredTop = it.height }
                .statusBarsPadding()
                .padding(EdgePadding),
            horizontalArrangement = Arrangement.spacedBy(EdgePadding),
        ) {
            Box(Modifier.weight(1f)) { status() }
            LayersButton(baseLayer = chromeState.baseLayer, onSelectBaseLayer = chrome::selectBaseLayer)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { coveredBottom = it.height },
            verticalArrangement = Arrangement.spacedBy(EdgePadding),
        ) {
            Column(
                modifier = Modifier.align(Alignment.End).padding(horizontal = EdgePadding),
                verticalArrangement = Arrangement.spacedBy(EdgePadding),
                horizontalAlignment = Alignment.End,
            ) {
                mapButtons()
                RecenterButton(following = chromeState.following, onClick = chrome::onWhereAmIClicked)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (sheet == null) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                    .padding(horizontal = EdgePadding)
                    .padding(bottom = if (sheet == null) EdgePadding else 0.dp),
                verticalArrangement = Arrangement.spacedBy(EdgePadding),
            ) {
                MapHints(chrome = chromeState, onOpenSettings = { context.openAppSettings() })
                bottom()
            }
            if (sheet != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = sheetMaxHeight)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = sheet,
                    )
                }
            }
        }
    }
}

private val StatusBarScrim = Color.Black.copy(alpha = 0.35f)
