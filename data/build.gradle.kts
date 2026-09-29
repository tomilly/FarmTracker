plugins {
    alias(libs.plugins.farmtracker.android.library)
    alias(libs.plugins.farmtracker.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pl.farmtracker.data"
}

dependencies {
    api(projects.coreDomain)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core.okio)
    implementation(libs.okio)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okio.fakefilesystem)
}
