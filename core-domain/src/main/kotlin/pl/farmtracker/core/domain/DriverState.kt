package pl.farmtracker.core.domain

/** Co teraz robi kierowca transportu (BRIEF §8, DriverStatus.state). */
enum class DriverState {
    IDLE,
    TO_FIELD,
    LOADING,
    TO_BASE,
    UNLOADING,
}
