package pl.farmtracker.app.navigation

import kotlinx.serialization.Serializable
import pl.farmtracker.core.domain.Role

/** Główne ekrany aplikacji. Każda rola ma własny ekran startowy – bez menu pośredniego. */
@Serializable
sealed interface AppDestination

@Serializable
data object RolePickerDestination : AppDestination

@Serializable
data object HarvesterDestination : AppDestination

@Serializable
data object DriverDestination : AppDestination

@Serializable
data object BaseDestination : AppDestination

@Serializable
data object AdminDestination : AppDestination

/** Ekran, na który trafia osoba o danej roli; bez roli – wybór roli (tymczasowo, do M3). */
fun Role?.toDestination(): AppDestination = when (this) {
    null -> RolePickerDestination
    Role.HARVESTER -> HarvesterDestination
    Role.DRIVER -> DriverDestination
    Role.BASE -> BaseDestination
    Role.ADMIN -> AdminDestination
}
