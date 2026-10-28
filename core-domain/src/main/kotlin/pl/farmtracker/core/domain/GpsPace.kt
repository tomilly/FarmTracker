package pl.farmtracker.core.domain

/**
 * Jak często pytać GPS – bateria na cały dzień zbioru. W ruchu co [FAST] (ładowanie przy sieczkarni potrzebuje
 * dokładnych, częstych pozycji); po [SLOW_AFTER_MILLIS] postoju w jednym miejscu (baza, czekanie na przyczepę) – co
 * [SLOW]: wciąż częściej niż raz na minutę, więc inni nie widzą „brak pozycji". Po ruszeniu – od razu znów szybko.
 */
enum class GpsPace(val intervalMillis: Long) {
    FAST(5_000),
    SLOW(30_000),
    ;

    companion object {
        const val SLOW_AFTER_MILLIS = 2 * 60_000L

        /** @param stillSinceMillis od kiedy telefon stoi w miejscu ([StillTracker]) */
        fun forStillSince(stillSinceMillis: Long, nowMillis: Long): GpsPace =
            if (nowMillis - stillSinceMillis >= SLOW_AFTER_MILLIS) SLOW else FAST
    }
}
