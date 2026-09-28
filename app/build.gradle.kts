plugins {
    alias(libs.plugins.farmtracker.android.application)
    alias(libs.plugins.farmtracker.android.compose)
    alias(libs.plugins.farmtracker.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pl.farmtracker.app"

    defaultConfig {
        applicationId = "pl.farmtracker.app"
        versionCode = 1
        versionName = "0.1.0"
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
    implementation(projects.featureFields)
    implementation(projects.featureMap)
    implementation(projects.featureRoles)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.coreTesting)
}
