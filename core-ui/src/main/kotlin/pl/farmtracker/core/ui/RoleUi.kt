package pl.farmtracker.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.ui.graphics.vector.ImageVector
import pl.farmtracker.core.domain.Role

/** Jak rola wygląda w UI: ikona + podpis (zawsze razem). */
object RoleUi {
    fun icon(role: Role): ImageVector = when (role) {
        Role.ADMIN -> Icons.Filled.ManageAccounts
        Role.HARVESTER -> Icons.Filled.Agriculture
        Role.DRIVER -> Icons.Filled.LocalShipping
        Role.BASE -> Icons.Filled.Warehouse
    }

    @StringRes
    fun labelRes(role: Role): Int = when (role) {
        Role.ADMIN -> R.string.core_ui_role_admin
        Role.HARVESTER -> R.string.core_ui_role_harvester
        Role.DRIVER -> R.string.core_ui_role_driver
        Role.BASE -> R.string.core_ui_role_base
    }
}
