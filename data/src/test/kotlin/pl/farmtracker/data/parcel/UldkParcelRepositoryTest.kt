package pl.farmtracker.data.parcel

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.farmtracker.core.domain.geo.GeoPoint
import pl.farmtracker.data.network.HttpGet
import java.io.IOException

class UldkParcelRepositoryTest {

    private val polygon = "SRID=4326;POLYGON((16.578 52.355,16.581 52.354,16.585 52.352,16.578 52.355))"

    private val byPoint = """
        0
        $polygon|302103_5.0007.125|powiat poznański|Buk|Otusz|125
    """.trimIndent()

    private val threeResults = """
        3
        $polygon|021504_2.0002.1|powiat oławski|Oława|Bystrzyca|1
        $polygon|060710_2.0001.1|powiat kraśnicki|Zakrzówek|Bystrzyca|1
        $polygon|181501_2.0002.1|powiat ropczycko-sędziszowski|Iwierzyce|Bystrzyca|1
    """.trimIndent()

    private val firewallPage = "<html><head><title>Request Rejected</title></head><body>rejected</body></html>"

    @Test
    fun `parcel by point is parsed with all its fields`() {
        val parcel = (UldkResponse.parse(byPoint) as UldkResult.Parcels).parcels.single()

        assertEquals("302103_5.0007.125", parcel.id)
        assertEquals("125", parcel.number)
        assertEquals("Otusz", parcel.precinct)
        assertEquals("Buk", parcel.commune)
        assertEquals("powiat poznański", parcel.county)
        assertEquals(GeoPoint(latitude = 52.355, longitude = 16.578), parcel.shape.single().outer.first())
    }

    @Test
    fun `search can return several parcels`() {
        val parcels = (UldkResponse.parse(threeResults) as UldkResult.Parcels).parcels

        assertEquals(listOf("Oława", "Zakrzówek", "Iwierzyce"), parcels.map { it.commune })
    }

    @Test
    fun `no results, errors, firewall page and garbage`() {
        assertEquals(UldkResult.NotFound, UldkResponse.parse("-1 brak wyników\n"))
        assertEquals(UldkResult.Error, UldkResponse.parse("-2 błąd usługi"))
        assertEquals(UldkResult.Error, UldkResponse.parse(""))
        assertEquals(UldkResult.Error, UldkResponse.parse(firewallPage))
        assertEquals(UldkResult.Error, UldkResponse.parse("1\nPOINT(1 2)|a|b|c|d|e"))
        assertEquals(UldkResult.Error, UldkResponse.parse("1\nniepelne|pola"))
    }

    @Test
    fun `point request sends lon lat order and asks for wgs84`() = runTest {
        var requested = ""
        val repository = UldkParcelRepository(HttpGet { url -> requested = url; byPoint })

        val result = repository.parcelAt(GeoPoint(latitude = 52.357, longitude = 16.5935))

        assertTrue(result is ParcelLookup.Found)
        assertTrue(requested, requested.contains("xy=16.5935000,52.3570000,4326"))
        assertTrue(requested, requested.contains("srid=4326"))
    }

    @Test
    fun `search encodes spaces as percent-20 so the server firewall accepts polish letters`() = runTest {
        var requested = ""
        val repository = UldkParcelRepository(HttpGet { url -> requested = url; threeResults })

        val result = repository.search("  Nowa Wieś 1 ")

        assertEquals(3, (result as ParcelSearch.Found).parcels.size)
        assertTrue(requested, requested.contains("request=GetParcelByIdOrNr&id=Nowa%20Wie%C5%9B%201&"))
        assertFalse(requested, requested.contains("+"))
    }

    @Test
    fun `empty search does not call the server`() = runTest {
        var called = false
        val repository = UldkParcelRepository(HttpGet { called = true; threeResults })

        assertEquals(ParcelSearch.NotFound, repository.search("   "))
        assertFalse(called)
    }

    @Test
    fun `network error means unavailable, not a crash`() = runTest {
        val repository = UldkParcelRepository(HttpGet { throw IOException("brak zasięgu") })

        assertEquals(ParcelLookup.Unavailable, repository.parcelAt(GeoPoint(52.0, 17.0)))
        assertEquals(ParcelSearch.Unavailable, repository.search("Otusz 125"))
    }
}
