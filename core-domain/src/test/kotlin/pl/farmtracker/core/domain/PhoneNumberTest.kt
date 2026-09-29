package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {

    @Test
    fun `polish numbers are accepted however they are typed`() {
        listOf("600123456", "600 123 456", "600-123-456", "+48 600 123 456", "0048600123456", "(+48) 600123456")
            .forEach { assertEquals(it, "+48600123456", normalizePhoneNumber(it)) }
    }

    @Test
    fun `foreign numbers keep their country code`() {
        assertEquals("+380501234567", normalizePhoneNumber("+380 50 123 4567"))
    }

    @Test
    fun `not a phone number`() {
        listOf("", "12345", "60012345a", "+48 600", "600 123 45").forEach { assertNull(it, normalizePhoneNumber(it)) }
    }
}
