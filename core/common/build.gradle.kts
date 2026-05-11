plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.alphadragon.core.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        create("staging") {
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
        release {
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.android)
    testImplementation(libs.bundles.testing.unit)
}
