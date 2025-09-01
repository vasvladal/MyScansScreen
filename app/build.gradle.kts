// ========== app/build.gradle.kts ==========
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.kropimagecropper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kropimagecropper"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Android Core
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.runtime.ktx)
    implementation(libs.android.activity.compose)

    // Compose BOM and UI
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // Krop Image Cropper - THE MAIN INTEGRATION
    implementation(libs.bundles.krop)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Image Loading
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)

    // Debug
    debugImplementation(libs.bundles.debug.testing)
}