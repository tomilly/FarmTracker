package pl.farmtracker.data.place

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pl.farmtracker.core.domain.Place
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.network.HttpGet
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

/**
 * Wyszukiwarka miejscowości OpenStreetMap (Nominatim). W przeciwieństwie do usług GUGiK znajduje
 * nazwy wpisane bez polskich znaków. Zasady usługi: przedstawienie się aplikacji (User-Agent
 * w [HttpGet]), nie więcej niż ~1 zapytanie/s i żadnego wyszukiwania w trakcie pisania – szukamy
 * dopiero po „Szukaj". Przy większej skali: własna instancja albo płatny dostawca.
 */
class NominatimPlaceRepository @Inject constructor(
    private val http: HttpGet,
) : PlaceRepository {

    override suspend fun search(query: String, near: GeoPoint?): PlaceSearch {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return PlaceSearch.NotFound
        return try {
            val places = NominatimResponse.parse(http.get(searchUrl(trimmed, near)))
            if (places.isEmpty()) PlaceSearch.NotFound else PlaceSearch.Found(places)
        } catch (_: IOException) {
            PlaceSearch.Unavailable
        } catch (_: SerializationException) {
            PlaceSearch.Unavailable
        }
    }

    internal companion object {
        private const val BASE_URL = "https://nominatim.openstreetmap.org/search"
        private const val LIMIT = 20

        /** Pół boku kwadratu (w stopniach), w którym wyniki są preferowane – ok. 100 km wokół [near]. */
        private const val NEAR_BOX_DEGREES = 1.0

        fun searchUrl(query: String, near: GeoPoint?): String {
            val q = URLEncoder.encode(query, Charsets.UTF_8.name()).replace("+", "%20")
            val nearBias = near?.let {
                String.format(
                    Locale.ROOT,
                    "&viewbox=%.4f,%.4f,%.4f,%.4f&bounded=0",
                    it.longitude - NEAR_BOX_DEGREES,
                    it.latitude + NEAR_BOX_DEGREES,
                    it.longitude + NEAR_BOX_DEGREES,
                    it.latitude - NEAR_BOX_DEGREES,
                )
            }.orEmpty()
            // featureType=settlement: tylko miasta, wsie, przysiółki – bez rzek i szczytów o tej samej nazwie.
            return "$BASE_URL?q=$q&countrycodes=pl&featureType=settlement&format=jsonv2&addressdetails=1" +
                "&accept-language=pl&limit=$LIMIT$nearBias"
        }
    }
}

internal object NominatimResponse {

    private val json = Json { ignoreUnknownKeys = true }

    /** @throws SerializationException gdy odpowiedź nie jest oczekiwanym JSON-em */
    fun parse(body: String): List<Place> =
        json.decodeFromString<List<NominatimItem>>(body).mapNotNull { it.toPlace() }

    @Serializable
    private data class NominatimItem(
        val name: String = "",
        val lat: String,
        val lon: String,
        val address: NominatimAddress = NominatimAddress(),
    ) {
        fun toPlace(): Place? {
            val latitude = lat.toDoubleOrNull() ?: return null
            val longitude = lon.toDoubleOrNull() ?: return null
            if (name.isBlank()) return null
            return Place(
                name = name,
                // Miasta na prawach powiatu nie mają gminy – wtedy pokazujemy samo miasto.
                commune = address.municipality ?: address.city ?: "",
                county = address.county ?: address.city ?: "",
                location = GeoPoint(latitude = latitude, longitude = longitude),
            )
        }
    }

    @Serializable
    private data class NominatimAddress(
        val municipality: String? = null,
        val county: String? = null,
        val city: String? = null,
    )
}
