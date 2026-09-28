package pl.farmtracker.data.parcel

import pl.farmtracker.core.domain.Parcel
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.core.domain.geo.Wkt
import pl.farmtracker.data.network.HttpGet
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

/** Działki z usługi GUGiK ULDK (uldk.gugik.gov.pl) – bez klucza, dane publiczne. */
class UldkParcelRepository @Inject constructor(
    private val http: HttpGet,
) : ParcelRepository {

    override suspend fun parcelAt(point: GeoPoint): ParcelLookup = try {
        UldkResponse.parse(http.get(urlFor(point)))
    } catch (_: IOException) {
        ParcelLookup.Unavailable
    }

    internal companion object {
        private const val BASE_URL = "https://uldk.gugik.gov.pl/"

        /** ULDK przyjmuje xy w kolejności długość,szerokość; wynik prosimy też w WGS84. */
        fun urlFor(point: GeoPoint): String {
            val xy = String.format(Locale.ROOT, "%.7f,%.7f", point.longitude, point.latitude)
            return "$BASE_URL?request=GetParcelByXY&xy=$xy,4326" +
                "&result=${UldkResponse.RESULT_FIELDS}&srid=4326"
        }
    }
}

/**
 * Odpowiedź ULDK: pierwsza linia to status (`0` = OK, `-1 brak wyników`, inne = błąd),
 * druga – pola rozdzielone `|` w kolejności z [RESULT_FIELDS].
 */
internal object UldkResponse {

    const val RESULT_FIELDS = "geom_wkt,id,commune,region,parcel"

    fun parse(body: String): ParcelLookup {
        val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val status = lines.firstOrNull() ?: return ParcelLookup.Unavailable
        return when {
            status.startsWith("-1") -> ParcelLookup.NotFound
            status != "0" -> ParcelLookup.Unavailable
            else -> lines.getOrNull(1)?.let(::parseParcel) ?: ParcelLookup.NotFound
        }
    }

    private fun parseParcel(line: String): ParcelLookup {
        val fields = line.split('|')
        if (fields.size < 5) return ParcelLookup.Unavailable
        val shape = try {
            Wkt.parsePolygons(fields[0])
        } catch (_: IllegalArgumentException) {
            // NumberFormatException też jest IllegalArgumentException.
            return ParcelLookup.Unavailable
        }
        return ParcelLookup.Found(
            Parcel(
                id = fields[1],
                commune = fields[2],
                precinct = fields[3],
                number = fields[4],
                shape = shape,
            ),
        )
    }
}
