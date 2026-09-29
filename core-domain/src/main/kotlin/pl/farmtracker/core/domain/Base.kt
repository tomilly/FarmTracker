package pl.farmtracker.core.domain

import pl.farmtracker.core.domain.geo.GeoPoint

/**
 * Baza zbioru – silos albo pryzma, gdzie kierowcy rozładowują kukurydzę. Na razie jedna na zbiór
 * (BRIEF: kilka baz to backlog).
 */
data class Base(val location: GeoPoint)
