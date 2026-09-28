package pl.farmtracker.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val White = Color(0xFFFFFFFF)
private val Ink = Color(0xFF111411)

// Jasny motyw jest domyślny: wysoki kontrast, czytelny w słońcu.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = White,
    primaryContainer = Color(0xFFD7EED8),
    onPrimaryContainer = Color(0xFF0A2E0D),
    secondary = Color(0xFF5D4037),
    onSecondary = White,
    background = White,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFECEFE9),
    onSurfaceVariant = Color(0xFF2B312A),
    outline = Color(0xFF3C4A3B),
    error = Color(0xFFB71C1C),
    onError = White,
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF388E3C),
    onPrimary = White,
    primaryContainer = Color(0xFF1B3A1D),
    onPrimaryContainer = Color(0xFFD7EED8),
    secondary = Color(0xFFD7CCC8),
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFF0F130F),
    onBackground = Color(0xFFF2F4F0),
    surface = Color(0xFF0F130F),
    onSurface = Color(0xFFF2F4F0),
    surfaceVariant = Color(0xFF263026),
    onSurfaceVariant = Color(0xFFDDE5DA),
    outline = Color(0xFFB7C4B4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1111),
)

/** Kolory znaczeniowe (BRIEF §4): zielony = OK/jedź, czerwony = stop/problem, żółty = uwaga. */
@Immutable
data class SemanticColors(
    val go: Color,
    val onGo: Color,
    val stop: Color,
    val onStop: Color,
    val warning: Color,
    val onWarning: Color,
)

internal val LightSemanticColors = SemanticColors(
    go = Color(0xFF1B7F2A),
    onGo = White,
    stop = Color(0xFFC62828),
    onStop = White,
    warning = Color(0xFFFFB300),
    onWarning = Ink,
)

internal val DarkSemanticColors = SemanticColors(
    go = Color(0xFF2E7D32),
    onGo = White,
    stop = Color(0xFFD32F2F),
    onStop = White,
    warning = Color(0xFFFFB300),
    onWarning = Ink,
)

internal val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }
