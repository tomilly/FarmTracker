package pl.farmtracker.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HarvestTest {

    private val owner = Member("u1", "Tomek", "+48600000001", Role.ADMIN)
    private val driver = Member("u2", "Marek", "+48600000002", Role.DRIVER)

    @Test
    fun `the only admin cannot be removed or demoted, others can`() {
        val members = listOf(owner, driver)

        assertFalse(members.canChangeOrRemove(owner))
        assertTrue(members.canChangeOrRemove(driver))
        assertTrue((members + owner.copy(userId = "u3")).canChangeOrRemove(owner))
    }

    @Test
    fun `invite codes are typed forgivingly and shown in two groups`() {
        assertEquals(InviteCode("482913"), InviteCode.parse(" 482 913 "))
        assertEquals(InviteCode("482913"), InviteCode.parse("482-913"))
        assertNull(InviteCode.parse("48291"))
        assertNull(InviteCode.parse("48291O")) // litera O zamiast zera
        assertEquals("482 913", InviteCode("482913").display)
    }

    @Test
    fun `random codes have six digits`() {
        val code = InviteCode.random(Random(7))

        assertEquals(6, code.digits.length)
        assertTrue(code.digits.all { it.isDigit() })
    }

    @Test
    fun `an invite stops working after it expires`() {
        val invite = Invite(InviteCode("482913"), "h1", Role.DRIVER, expiresAtMillis = 1_000)

        assertTrue(invite.isValidAt(999))
        assertFalse(invite.isValidAt(1_000))
    }
}
