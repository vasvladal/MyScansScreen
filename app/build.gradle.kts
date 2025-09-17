@file:Suppress("UnstableApiUsage")
import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Securely load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.canRead()) {
        load(FileInputStream(versionPropsFile))
    }
}

val versionMajor = versionProps.getProperty("VERSION_MAJOR", "14").toInt()
val versionMinor = versionProps.getProperty("VERSION_MINOR", "1").toInt()
val versionPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val versionBuild = versionProps.getProperty("VERSION_BUILD", "1").toInt()

// Create the version name strings
val appVersionName = "$versionMajor.$versionMinor.$versionPatch.$versionBuild"
val appVersionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild

// Copy version.properties to app assets
tasks.register<Copy>("copyVersionProperties") {
    from(versionPropsFile)
    into("src/main/assets")
}

android {
    namespace = "com.example.kropimagecropper"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.kropimagecropper"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("int", "VERSION_CODE", "$appVersionCode")
        buildConfigField("String", "BUILD_DATE", "\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\"")

        // Limit to specific ABIs to reduce APK size
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties["storeFile"]?.let { file(it as String) }
            storePassword = keystoreProperties["storePassword"] as? String ?: ""
            keyAlias = keystoreProperties["keyAlias"] as? String ?: ""
            keyPassword = keystoreProperties["keyPassword"] as? String ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/NOTICE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "**/x86/libopencv_java4.so",
                "**/x86_64/libopencv_java4.so"
            )

            pickFirsts += setOf(
                "**/libc++_shared.so",
                "**/libopencv_java4.so"
            )
        }
    }

    // Configure APK naming
    applicationVariants.all {
        val variant = this
        val buildType = variant.buildType.name
        val flavorName = variant.flavorName.takeIf { it.isNotBlank() }?.let { "$it-" } ?: ""
        val version = appVersionName.replace(".", "_")
        val date = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())

        variant.outputs.all {
            val output = this
            val projectName = "DocumentScanner"

            if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                output.outputFileName = "${projectName}_${flavorName}${version}_${variant.versionCode}_${buildType}_${date}.apk"
            }
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Force consistent versions only where necessary
        force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
        force("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
    }
    // Exclude the conflicting annotations-java5 module
    exclude(group = "org.jetbrains", module = "annotations-java5")
}

dependencies {
    // Android Core
    implementation(libs.android.core.ktx)
    implementation(libs.android.lifecycle.runtime.ktx)
    implementation(libs.android.activity.compose)

    // Compose BOM and UI
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // Krop Image Cropper
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

    // Debug
    debugImplementation(libs.bundles.debug.testing)

    implementation(libs.androidx.profileinstaller)

    // Markwon libraries with exclusions
    implementation(libs.markwon.core) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    implementation(libs.markwon.html) {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // OpenCV with exclusion
    implementation(libs.opencv.opencv) {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    implementation(libs.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.gpuimage)
}

// Version increment task
tasks.register("incrementVersionCode") {
    doLast {
        if (gradle.startParameter.taskNames.any { it.contains("assembleRelease") }) {
            val newBuildNumber = (versionBuild + 1).toString()
            versionProps.setProperty("VERSION_BUILD", newBuildNumber)
            versionProps.store(versionPropsFile.outputStream(), null)
            println("Version code incremented to $newBuildNumber for Release build.")
        } else {
            println("Version code not incremented. Current build is not Release.")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("copyVersionProperties")
}

// Connect version increment to release build
afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy("incrementVersionCode")
    }
}