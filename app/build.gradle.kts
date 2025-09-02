@file:Suppress("UnstableApiUsage")
import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.tasks.Exec
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

println("Version Properties File Path: ${versionPropsFile.absolutePath}")
println("Version Properties File Exists: ${versionPropsFile.exists()}")
println("Version Properties File Can Read: ${versionPropsFile.canRead()}")

if (versionPropsFile.exists()) {
    println("Version Properties File Contents:")
    try {
        println(versionPropsFile.readText())
    } catch (e: Exception) {
        println("Error reading file: ${e.message}")
    }
}

val versionProps = Properties().apply {
    if (versionPropsFile.canRead()) {
        load(FileInputStream(versionPropsFile))
    } else {
        println("Cannot read version properties file!")
    }
}

val versionMajor = versionProps.getProperty("VERSION_MAJOR", "14").toInt()
val versionMinor = versionProps.getProperty("VERSION_MINOR", "1").toInt()
val versionPatch = versionProps.getProperty("VERSION_PATCH", "0").toInt()
val versionBuild = versionProps.getProperty("VERSION_BUILD", "1").toInt()

// Create the version name strings
val appVersionName =
    "$versionMajor.$versionMinor.$versionPatch.$versionBuild"  // Short version (without build number)
val appVersionNameFull =
    "$versionMajor.$versionMinor.$versionPatch.$versionBuild"  // Full version
// Calculate version code (should be a unique incrementing number)
// Using a formula: major*10000 + minor*1000 + patch*100 + build
val appVersionCode =
    versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild

println("Generated versionName: $appVersionName")
println("Generated versionCode: $appVersionCode")

// Copy version.properties to app assets
tasks.register<Copy>("copyVersionProperties") {
    from(versionPropsFile)
    into("src/main/assets")
    doLast {
        println("Copied version.properties to assets folder")
        // Print the contents of the copied file for verification
        val copiedFile = file("src/main/assets/version.properties")
        if (copiedFile.exists()) {
            println("Copied file contents: ${copiedFile.readText()}")
        } else {
            println("WARNING: Copied file not found!")
        }
    }
}

android {
    namespace = "com.example.kropimagecropper"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.kropimagecropper.v2"  // Changed package ID temporarily
        minSdk = 29
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName  // Use short version here

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
// Add version info to BuildConfig
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("String", "VERSION_NAME_FULL", "\"$appVersionNameFull\"")
        buildConfigField("int", "VERSION_CODE", "$appVersionCode")
        buildConfigField("String", "BUILD_DATE", "\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties["storeFile"]?.let { file(it as String) }

            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: keystoreProperties["storePassword"] as? String
                        ?: error("Keystore password not found")

            keyAlias = keystoreProperties["keyAlias"] as? String
                ?: error("Key alias not found")

            keyPassword = System.getenv("KEY_PASSWORD")
                ?: keystoreProperties["keyPassword"] as? String
                        ?: error("Key password not found")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true  // Enable BuildConfig generation
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
                "META-INF/INDEX.LIST"
            )
            merges += setOf("META-INF/LICENSE")
        }
    }

    // Custom APK/AAB output file naming
    applicationVariants.all {
        val variant = this
        val buildType = variant.buildType.name
        val flavorName = variant.flavorName.takeIf { it.isNotBlank() }?.let { "$it-" } ?: ""
        val version = appVersionName.replace(".", "_")
        val date = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())

        variant.outputs.all {
            val output = this
            val projectName = "ClipboardMonitor"

            when (output) {
                is com.android.build.gradle.internal.api.ApkVariantOutputImpl -> {
                    output.outputFileName = "${projectName}_${flavorName}${version}_${variant.versionCode}_${buildType}_${date}.apk"
                }
//                is com.android.build.gradle.internal.api.BundleVariantOutputImpl -> {
//                    output.outputFileName = "${projectName}_${flavorName}${version}_${variant.versionCode}_${buildType}_${date}.aab"
//                }
            }
        }
    }
}

// Global dependency resolution strategy to handle annotation conflicts
configurations.all {
    resolutionStrategy {
        // Keep this if needed for other dependencies
        force("org.jetbrains:annotations:26.0.2")
    }
    // Remove these lines:
     exclude(group = "org.jetbrains", module = "annotations-java5")
    // exclude(group = "com.google.guava", module = "listenablefuture")
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

    // Concurrent Futures - Fix for ProfileInstaller issue
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.androidx.appcompat)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.bundles.android.testing)

    // Debug
    debugImplementation(libs.bundles.debug.testing)

    implementation(libs.androidx.profileinstaller)


    // Markwon for markdown rendering with exclusions
    implementation(libs.markwon.core) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation(libs.markwon.html) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

    // Optional syntax highlighting
    implementation(libs.markwon.syntax) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }
    implementation(libs.prism4j) {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

    // Image support for Markwon
    implementation("io.noties.markwon:image:4.6.2") {
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "org.jetbrains", module = "annotations-java5")
    }

}

// Task to increment version code
tasks.register("incrementVersionCode") {
    doLast {
        // Check if the current task being executed is 'assembleRelease'
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

// Make sure version.properties is copied to assets during build
tasks.named("preBuild") {
    dependsOn("copyVersionProperties")
}

// Hook to run incrementVersionCode after assembleRelease
afterEvaluate {
    tasks.named("assembleRelease") {
        finalizedBy("incrementVersionCode")
    }
}