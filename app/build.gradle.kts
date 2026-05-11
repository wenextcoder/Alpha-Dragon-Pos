plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.alphadragon.pos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.alphadragon.pos"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // OTA update manifest URL — no secrets, just a public endpoint
        buildConfigField("String", "UPDATE_MANIFEST_URL", "\"https://your-host/version.json\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        create("staging") {
            isDebuggable = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room schema export for migration history tracking
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // JUnit 5 jars each embed META-INF/LICENSE.md — merge needs a single winner
            pickFirsts += "META-INF/LICENSE.md"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:payment-interface"))

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room + SQLCipher
    implementation(libs.bundles.room)
    implementation(libs.sqlcipher.android)
    implementation(libs.sqlite.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Security
    implementation(libs.jbcrypt)
    implementation(libs.rootbeer)
    implementation(libs.security.crypto)

    // Image loading
    implementation(libs.coil.compose)

    // Camera barcode scanning
    implementation(libs.bundles.camera)
    implementation(libs.mlkit.barcode.scanning)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Networking (for merchant APIs + OTA update check)
    implementation(libs.bundles.retrofit)
    implementation(libs.okhttp.logging)

    // Testing
    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.testing.android)
}
