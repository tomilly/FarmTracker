plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.hilt)
}

android {
    namespace = "pl.farmtracker.data"
}

dependencies {
    api(projects.coreDomain)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.androidx.datastore.core.okio)
    testImplementation(libs.okio.fakefilesystem)
}
