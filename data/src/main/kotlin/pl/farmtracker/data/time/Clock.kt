package pl.farmtracker.data.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Bieżący czas – osobno, żeby testy mogły go ustawiać. */
fun interface Clock {
    fun nowMillis(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** Czas odświeżany co [periodMillis] – np. żeby pozycja bez nowych danych sama zrobiła się „dawna". */
fun Clock.ticks(periodMillis: Long = 30_000): Flow<Long> = flow {
    while (true) {
        emit(nowMillis())
        delay(periodMillis)
    }
}
