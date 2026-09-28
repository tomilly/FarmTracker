plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.android.compose)
    alias(libs.plugins.farmtracker.hilt)
}

android {
    namespace = "pl.farmtracker.feature.map"
}

dependencies {
    implementation(projects.coreUi)
    implementation(libs.maplibre.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(projects.coreTesting)
}
