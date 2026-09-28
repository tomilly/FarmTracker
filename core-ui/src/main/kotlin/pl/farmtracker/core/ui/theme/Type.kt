package pl.farmtracker.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

// Powiększona typografia: czytelna z odległości ręki, w kabinie i w rękawicach.
internal val FarmTrackerTypography = Typography(
    headlineMedium = Base.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = Base.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    labelMedium = Base.labelMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
)
