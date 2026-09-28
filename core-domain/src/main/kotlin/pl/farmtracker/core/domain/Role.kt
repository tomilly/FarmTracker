package pl.farmtracker.core.domain

/** Rola osoby w danym zbiorze. Jedna osoba ma jedną rolę na zbiór (BRIEF §3). */
enum class Role {
    ADMIN,
    HARVESTER,
    DRIVER,
    BASE,
}
