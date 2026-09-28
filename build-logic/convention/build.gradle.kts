plugins {
    `kotlin-dsl`
}

group = "pl.farmtracker.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.farmtracker.android.application.get().pluginId
            implementationClass = "pl.farmtracker.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.farmtracker.android.library.get().pluginId
            implementationClass = "pl.farmtracker.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.farmtracker.android.compose.get().pluginId
            implementationClass = "pl.farmtracker.buildlogic.AndroidComposeConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.farmtracker.hilt.get().pluginId
            implementationClass = "pl.farmtracker.buildlogic.HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.farmtracker.jvm.library.get().pluginId
            implementationClass = "pl.farmtracker.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
