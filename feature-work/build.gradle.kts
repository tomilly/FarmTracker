plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.hilt)
}

android {
    namespace = "pl.farmtracker.feature.work"
}

dependencies {
    implementation(projects.data)
    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(projects.coreTesting)
}
