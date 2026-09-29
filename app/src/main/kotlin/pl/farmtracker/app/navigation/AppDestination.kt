package pl.farmtracker.app.navigation

import kotlinx.serialization.Serializable
import pl.farmtracker.core.domain.Role

/** Główne ekrany aplikacji. Każda rola ma własny ekran startowy – bez menu pośredniego. */
@Serializable
sealed interface AppDestination

/** Logowanie numerem telefonu (wspólny zbiór). */
@Serializable
data object LoginDestination : AppDestination

/** Po zalogowaniu, bez zbioru: imię, kod zaproszenia albo nowy zbiór. */
@Serializable
data object OnboardingDestination : AppDestination

/** Wybór roli: bez Firebase – zamiast logowania; z Firebase – tylko w wersji testowej („Zmień rolę"). */
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

/** Mapa otwierana z ekranu roli („Mapa"); nie jest ekranem startowym żadnej roli. */
@Serializable
data object MapDestination

/** Ludzie w zbiorze i zaproszenia (admin → „Ludzie"). */
@Serializable
data object TeamDestination

/** Lista pól zbioru (admin → „Pola"). */
@Serializable
data object FieldsListDestination

/** Ustawianie bazy zbioru – silosu / pryzmy (admin → „Baza"). Nie mylić z ekranem roli [BaseDestination]. */
@Serializable
data object BaseSetupDestination

/** Jedno pole na mapie (dotknięte na liście); stąd „Edytuj pole". Nazwa właściwości = `FieldViewModel.FIELD_ID_ARG`. */
@Serializable
data class FieldDestination(val fieldId: String)

/**
 * Tworzenie nowego pola (`fieldId == null`) albo edycja istniejącego. Nazwa właściwości musi się
 * zgadzać z `FieldEditorViewModel.FIELD_ID_ARG`.
 */
@Serializable
data class FieldEditorDestination(val fieldId: String? = null)

/** Ekran, na który trafia osoba o danej roli; bez roli – wybór roli (tymczasowo, do M3). */
fun Role?.toDestination(): AppDestination = when (this) {
    null -> RolePickerDestination
    Role.HARVESTER -> HarvesterDestination
    Role.DRIVER -> DriverDestination
    Role.BASE -> BaseDestination
    Role.ADMIN -> AdminDestination
}
