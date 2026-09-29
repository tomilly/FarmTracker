package pl.farmtracker.data.place

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.Place
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.network.HttpGet
import java.io.IOException

class NominatimPlaceRepositoryTest {

    private val sulmow = """
        [{"place_id":1,"lat":"51.7915399","lon":"18.4561496","name":"Sulmów","addresstype":"village",
          "address":{"village":"Sulmów","municipality":"gmina Goszczanów","county":"powiat sieradzki",
          "state":"województwo łódzkie","country":"Polska"}},
         {"place_id":2,"lat":"50.0","lon":"19.0","name":"","address":{}},
         {"place_id":3,"lat":"51.1","lon":"17.03","name":"Wrocław",
          "address":{"city":"Wrocław","state":"województwo dolnośląskie"}}]
    """.trimIndent()

    @Test
    fun `places are parsed, nameless ones skipped, cities fill commune and county`() {
        val places = NominatimResponse.parse(sulmow)

        assertEquals(
            listOf(
                Place("Sulmów", "gmina Goszczanów", "powiat sieradzki", GeoPoint(51.7915399, 18.4561496)),
                Place("Wrocław", "Wrocław", "Wrocław", GeoPoint(51.1, 17.03)),
            ),
            places,
        )
    }

    @Test
    fun `query without polish letters is sent as is, settlements only, biased to the map`() = runTest {
        var requested = ""
        val repository = NominatimPlaceRepository(HttpGet { url -> requested = url; sulmow })

        val result = repository.search(" sulmow ", near = GeoPoint(51.0, 17.4))

        assertTrue(result is PlaceSearch.Found)
        assertTrue(requested, requested.contains("q=sulmow&"))
        assertTrue(requested, requested.contains("countrycodes=pl"))
        assertTrue(requested, requested.contains("featureType=settlement"))
        assertTrue(requested, requested.contains("viewbox=16.4000,52.0000,18.4000,50.0000&bounded=0"))
    }

    @Test
    fun `spaces and polish letters are percent encoded without plus`() = runTest {
        var requested = ""
        val repository = NominatimPlaceRepository(HttpGet { url -> requested = url; sulmow })

        repository.search("Nowa Wieś", near = null)

        assertTrue(requested, requested.contains("q=Nowa%20Wie%C5%9B&"))
        assertFalse(requested, requested.contains("viewbox"))
    }

    @Test
    fun `empty answer, garbage and no signal`() = runTest {
        assertEquals(PlaceSearch.NotFound, NominatimPlaceRepository(HttpGet { "[]" }).search("xyz", null))
        assertEquals(PlaceSearch.Unavailable, NominatimPlaceRepository(HttpGet { "<html>" }).search("xyz", null))
        assertEquals(
            PlaceSearch.Unavailable,
            NominatimPlaceRepository(HttpGet { throw IOException("brak zasięgu") }).search("xyz", null),
        )
        assertEquals(PlaceSearch.NotFound, NominatimPlaceRepository(HttpGet { error("nie wołać") }).search("  ", null))
    }
}
