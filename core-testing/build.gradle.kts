plugins {
    alias(libs.plugins.farmtracker.android.library)
}

android {
    namespace = "pl.farmtracker.core.testing"
}

dependencies {
    api(projects.data)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
