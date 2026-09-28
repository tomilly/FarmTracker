plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.android.compose)
}

android {
    namespace = "pl.farmtracker.core.map"
}

dependencies {
    api(projects.coreUi)
    implementation(libs.maplibre.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
