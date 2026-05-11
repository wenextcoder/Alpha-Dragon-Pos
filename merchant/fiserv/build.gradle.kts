plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.alphadragon.merchant.fiserv"
    compileSdk = 35

    defaultConfig { minSdk = 24 }

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
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.android)
    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.testing.unit)
}
