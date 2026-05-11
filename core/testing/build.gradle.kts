plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.alphadragon.core.testing"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:payment-interface"))
    implementation(libs.coroutines.test)
    implementation(libs.mockk)
    implementation(libs.junit)
    implementation(libs.turbine)
}
