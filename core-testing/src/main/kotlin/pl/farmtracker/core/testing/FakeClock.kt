package pl.farmtracker.core.testing

import pl.farmtracker.data.time.Clock

/** Czas ustawiany w teście. */
class FakeClock(var now: Long = 0) : Clock {
    override fun nowMillis(): Long = now
}
