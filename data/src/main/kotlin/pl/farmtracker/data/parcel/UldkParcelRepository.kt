package pl.farmtracker.data.parcel

import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.Wkt
import pl.farmtracker.data.network.HttpGet
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

/** Działki z usługi GUGiK ULDK (uldk.gugik.gov.pl) – bez klucza, dane publiczne. */
class UldkParcelRepository @Inject constructor(
    private val http: HttpGet,
) : ParcelRepository {

    override suspend fun parcelAt(point: GeoPoint): ParcelLookup = try {
        when (val result = UldkResponse.parse(http.get(pointUrl(point)))) {
            is UldkResult.Parcels -> ParcelLookup.Found(result.parcels.first())
            UldkResult.NotFound -> ParcelLookup.NotFound
            UldkResult.Error -> ParcelLookup.Unavailable
        }
    } catch (_: IOException) {
        ParcelLookup.Unavailable
    }

    override suspend fun search(query: String): ParcelSearch {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ParcelSearch.NotFound
        return try {
            when (val result = UldkResponse.parse(http.get(searchUrl(trimmed)))) {
                is UldkResult.Parcels -> ParcelSearch.Found(result.parcels)
                UldkResult.NotFound -> ParcelSearch.NotFound
                UldkResult.Error -> ParcelSearch.Unavailable
            }
        } catch (_: IOException) {
            ParcelSearch.Unavailable
        }
    }

    internal companion object {
        private const val BASE_URL = "https://uldk.gugik.gov.pl/"

        /** ULDK przyjmuje xy w kolejności długość,szerokość; wynik prosimy też w WGS84. */
        fun pointUrl(point: GeoPoint): String {
            val xy = String.format(Locale.ROOT, "%.7f,%.7f", point.longitude, point.latitude)
            return "$BASE_URL?request=GetParcelByXY&xy=$xy,4326" +
                "&result=${UldkResponse.RESULT_FIELDS}&srid=4326"
        }

        /**
         * Spacja musi być zakodowana jako `%20`: zapora serwera odrzuca zapytania, w których `+`
         * (tak koduje spacje [URLEncoder]) występuje razem z polskimi znakami („Nowa Wieś 1").
         */
        fun searchUrl(query: String): String {
            val id = URLEncoder.encode(query, Charsets.UTF_8.name()).replace("+", "%20")
            return "$BASE_URL?request=GetParcelByIdOrNr&id=$id&result=${UldkResponse.RESULT_FIELDS}&srid=4326"
        }
    }
}

internal sealed interface UldkResult {
    data class Parcels(val parcels: List<Parcel>) : UldkResult
    data object NotFound : UldkResult
    data object Error : UldkResult
}

/**
 * Odpowiedź ULDK: pierwsza linia to status – liczba wyników (`0` przy zapytaniu o punkt, `1`, `3`…),
 * `-1 brak wyników` albo inny błąd; kolejne linie to działki, pola rozdzielone `|` w kolejności
 * z [RESULT_FIELDS]. Zamiast odpowiedzi może przyjść strona HTML zapory – traktujemy ją jak błąd.
 */
internal object UldkResponse {

    const val RESULT_FIELDS = "geom_wkt,id,county,commune,region,parcel"

    fun parse(body: String): UldkResult {
        val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val status = lines.firstOrNull() ?: return UldkResult.Error
        if (status.startsWith("-1")) return UldkResult.NotFound
        if (status.toIntOrNull()?.let { it >= 0 } != true) return UldkResult.Error
        val parcels = lines.drop(1).map { parseParcel(it) ?: return UldkResult.Error }
        return if (parcels.isEmpty()) UldkResult.NotFound else UldkResult.Parcels(parcels)
    }

    private fun parseParcel(line: String): Parcel? {
        val fields = line.split('|')
        if (fields.size < 6) return null
        val shape = try {
            Wkt.parsePolygons(fields[0])
        } catch (_: IllegalArgumentException) {
            // NumberFormatException też jest IllegalArgumentException.
            return null
        }
        return Parcel(
            id = fields[1],
            county = fields[2],
            commune = fields[3],
            precinct = fields[4],
            number = fields[5],
            shape = shape,
        )
    }
}
