plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.android.compose)
}

android {
    namespace = "pl.farmtracker.core.ui"
}

dependencies {
    api(projects.coreDomain)
    api(libs.androidx.compose.material.icons.extended)
}
