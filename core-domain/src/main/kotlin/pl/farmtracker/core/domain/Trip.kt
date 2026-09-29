package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.distanceMeters

/**
 * Co teraz robi kierowca – wykrywa to telefon z pozycji, kierowca nic nie klika (BRIEF §4).
 * Kurs: pole (ładowanie przy sieczkarni) → baza (rozładunek) → pole…
 */
enum class Trip {
    /** Jedzie po ładunek – opuścił bazę albo zawrócił w stronę pól. */
    TO_FIELD,

    /** Na polu, ale nie przy sieczkarni (czeka, wjeżdża, zjeżdża). */
    ON_FIELD,

    /** Jedzie równo przy sieczkarni – ładuje. */
    LOADING,

    /** Oddalił się od pola / sieczkarni – wraca do bazy. */
    TO_BASE,

    /** Przy bazie – rozładunek. */
    AT_BASE,
}

/**
 * Status kursu kierowcy z kolejnych pozycji. Pamięta poprzedni stan: kto zjechał z pola, wraca do bazy; kto
 * odjechał z bazy, jedzie na pole. W drodze liczy się też kierunek – kto zawrócił i oddala się od bazy,
 * jedzie z powrotem na pole (i odwrotnie).
 */
class TripTracker {

    var trip: Trip? = null
        private set

    /** Punkt, od którego mierzymy, czy kierowca zbliża się do bazy, czy od niej oddala. */
    private var reference: GeoPoint? = null
    private var fixesNextToHarvester = 0

    /**
     * @param fieldId pole pod kierowcą ([fieldWith])
     * @param harvesters świeże pozycje sieczkarni
     */
    fun update(point: GeoPoint, fieldId: String?, base: GeoPoint?, harvesters: List<GeoPoint>): Trip? {
        val loadingRadius = if (trip == Trip.LOADING) LOADING_EXIT_METERS else LOADING_ENTER_METERS
        val nextToHarvester = harvesters.any { it.distanceMeters(point) <= loadingRadius }
        fixesNextToHarvester = if (nextToHarvester) fixesNextToHarvester + 1 else 0
        val baseRadius = if (trip == Trip.AT_BASE) BASE_EXIT_METERS else BASE_ENTER_METERS

        val next = when {
            // Samo minięcie sieczkarni to jeszcze nie ładowanie – kierowca musi jechać przy niej przez dwie pozycje.
            nextToHarvester && (trip == Trip.LOADING || fixesNextToHarvester >= FIXES_TO_START_LOADING) -> Trip.LOADING
            // Baza przed polem – silos bywa na skraju pola przy domu.
            base != null && point.distanceMeters(base) <= baseRadius -> Trip.AT_BASE
            fieldId != null -> Trip.ON_FIELD
            trip == Trip.LOADING || trip == Trip.ON_FIELD -> Trip.TO_BASE
            trip == Trip.AT_BASE -> Trip.TO_FIELD
            else -> directionTo(base, point) ?: trip
        }
        if (next != trip) reference = point
        trip = next
        return next
    }

    /** W drodze: czy kierowca od [reference] zbliżył się do bazy, czy się od niej oddalił – o kawałek drogi. */
    private fun directionTo(base: GeoPoint?, point: GeoPoint): Trip? {
        if (base == null) return null
        val from = reference ?: point.also { reference = it }
        val change = from.distanceMeters(base) - point.distanceMeters(base)
        val direction = when {
            change >= DIRECTION_METERS -> Trip.TO_BASE
            -change >= DIRECTION_METERS -> Trip.TO_FIELD
            else -> return null
        }
        reference = point
        return direction
    }

    companion object {
        const val LOADING_ENTER_METERS = 40.0
        const val LOADING_EXIT_METERS = 80.0
        const val FIXES_TO_START_LOADING = 2
        const val BASE_ENTER_METERS = 150.0
        const val BASE_EXIT_METERS = 250.0

        /** Tyle drogi w stronę bazy (albo od niej), żeby uznać kierunek – GPS i zakręty nie przełączają statusu. */
        const val DIRECTION_METERS = 300.0
    }
}
