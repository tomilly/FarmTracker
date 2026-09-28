package pl.farmtracker.data.parcel

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.network.HttpGet
import java.io.IOException

class UldkParcelRepositoryTest {

    private val found = """
        0
        SRID=4326;POLYGON((16.578 52.355,16.581 52.354,16.585 52.352,16.578 52.355))|302103_5.0007.125|Buk|Otusz|125
    """.trimIndent()

    @Test
    fun `found parcel is parsed with its shape`() {
        val result = UldkResponse.parse(found)

        val parcel = (result as ParcelLookup.Found).parcel
        assertEquals("302103_5.0007.125", parcel.id)
        assertEquals("125", parcel.number)
        assertEquals("Otusz", parcel.precinct)
        assertEquals("Buk", parcel.commune)
        assertEquals(4, parcel.shape.single().outer.size)
        assertEquals(GeoPoint(latitude = 52.355, longitude = 16.578), parcel.shape.single().outer.first())
    }

    @Test
    fun `no results means not found`() {
        assertEquals(ParcelLookup.NotFound, UldkResponse.parse("-1 brak wyników\n"))
    }

    @Test
    fun `other status or garbage means unavailable`() {
        assertEquals(ParcelLookup.Unavailable, UldkResponse.parse("-2 błąd usługi"))
        assertEquals(ParcelLookup.Unavailable, UldkResponse.parse(""))
        assertEquals(ParcelLookup.Unavailable, UldkResponse.parse("0\nPOINT(1 2)|a|b|c|d"))
        assertEquals(ParcelLookup.Unavailable, UldkResponse.parse("0\nniepelne|pola"))
    }

    @Test
    fun `request sends lon lat order and asks for wgs84`() = runTest {
        var requested = ""
        val repository = UldkParcelRepository(HttpGet { url -> requested = url; found })

        val result = repository.parcelAt(GeoPoint(latitude = 52.357, longitude = 16.5935))

        assertTrue(result is ParcelLookup.Found)
        assertTrue(requested, requested.contains("xy=16.5935000,52.3570000,4326"))
        assertTrue(requested, requested.contains("srid=4326"))
    }

    @Test
    fun `network error means unavailable, not a crash`() = runTest {
        val repository = UldkParcelRepository(HttpGet { throw IOException("brak zasięgu") })

        assertEquals(ParcelLookup.Unavailable, repository.parcelAt(GeoPoint(52.0, 17.0)))
    }
}
