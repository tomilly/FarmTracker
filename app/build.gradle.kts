plugins {
    alias(libs.plugins.farmtracker.android.application)
    alias(libs.plugins.farmtracker.android.compose)
    alias(libs.plugins.farmtracker.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Firebase (logowanie, wspólny zbiór): plik z konsoli Firebase nie jest w repozytorium (repo publiczne).
// Bez niego aplikacja się buduje (CI, testy) i działa jak dotąd – dane tylko na telefonie, wybór roli.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "pl.farmtracker.app"

    defaultConfig {
        applicationId = "pl.farmtracker.app"
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("boolean", "SHARED_HARVEST", firebaseConfigured.toString())
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.coreUi)
    implementation(projects.data)
    implementation(projects.featureAuth)
    implementation(projects.featureFields)
    implementation(projects.featureMap)
    implementation(projects.featureRoles)
    implementation(projects.featureTeam)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.coreTesting)
}
