plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.android.compose)
    alias(libs.plugins.farmtracker.hilt)
}

android {
    namespace = "pl.farmtracker.feature.auth"
}

dependencies {
    implementation(projects.coreUi)
    implementation(projects.data)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(projects.coreTesting)
}
