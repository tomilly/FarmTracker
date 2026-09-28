package pl.farmtracker.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.farmtracker.core.domain.Role

class AppDestinationTest {

    @Test
    fun `each role opens its own screen`() {
        assertEquals(HarvesterDestination, Role.HARVESTER.toDestination())
        assertEquals(DriverDestination, Role.DRIVER.toDestination())
        assertEquals(BaseDestination, Role.BASE.toDestination())
        assertEquals(AdminDestination, Role.ADMIN.toDestination())
    }

    @Test
    fun `no role opens role picker`() {
        assertEquals(RolePickerDestination, null.toDestination())
    }
}
