package com.example.kropimagecropper.utils

import android.content.Context
import android.util.Log
import java.io.IOException
import java.lang.System.getProperty
import java.util.Properties

/**
 * Utility class to retrieve and format application version information.
 * Reads version details from version.properties in the assets folder and formats
 * according to the specification: MAJOR.MINOR.PATCH.BUILD
 */
object AppInfo {
    private const val TAG = "AppInfo"
    private const val VERSION_PROPERTIES_FILE = "version.properties"

    // Version component keys
    private object VersionKeys {
        const val MAJOR = "major"
        const val MINOR = "minor"
        const val PATCH = "patch"
        const val BUILD = "build"
    }

    // Property name variants
    private object PropertyNames {
        const val VERSION_MAJOR = "VERSION_MAJOR"
        const val VERSION_MINOR = "VERSION_MINOR"
        const val VERSION_PATCH = "VERSION_PATCH"
        const val VERSION_BUILD = "VERSION_BUILD"
        const val VERSION_MAJOR_ALT = "version.major"
        const val VERSION_MINOR_ALT = "version.minor"
        const val VERSION_PATCH_ALT = "version.patch"
        const val VERSION_BUILD_ALT = "version.build"
    }

    // Default version components in case reading fails
    private const val DEFAULT_MAJOR = "1"
    private const val DEFAULT_MINOR = "1"
    private const val DEFAULT_PATCH = "0"
    private const val DEFAULT_BUILD = "1"

    /**
     * Returns the formatted version string in format MAJOR.MINOR.PATCH.BUILD
     * Falls back to default values if any part of the process fails.
     *
     * @param context The application context
     * @return Formatted version string
     */
    fun getFormattedVersion(context: Context): String {
        val versionComponents = readVersionComponents(context)

        val major = versionComponents[VersionKeys.MAJOR] ?: DEFAULT_MAJOR
        val minor = versionComponents[VersionKeys.MINOR] ?: DEFAULT_MINOR
        val patch = versionComponents[VersionKeys.PATCH] ?: DEFAULT_PATCH
        val build = versionComponents[VersionKeys.BUILD] ?: DEFAULT_BUILD

        return "$major.$minor.$patch.$build"
    }

    /**
     * Reads version components from version.properties file in assets.
     *
     * @param context The application context
     * @return Map containing the version components (major, minor, patch, build)
     */
    private fun readVersionComponents(context: Context): Map<String, String> {
        val versionMap = mutableMapOf<String, String>().apply {
            // Set default values first
            put(VersionKeys.MAJOR, DEFAULT_MAJOR)
            put(VersionKeys.MINOR, DEFAULT_MINOR)
            put(VersionKeys.PATCH, DEFAULT_PATCH)
            put(VersionKeys.BUILD, DEFAULT_BUILD)
        }

        try {
            val properties = Properties().apply {
                context.assets.open(VERSION_PROPERTIES_FILE).use { load(it) }
            }

            // Read version components with fallback to alternative property names
            versionMap[VersionKeys.MAJOR] = properties.getProperty(
                PropertyNames.VERSION_MAJOR,
                properties.getProperty(PropertyNames.VERSION_MAJOR_ALT, DEFAULT_MAJOR)
            )
            versionMap[VersionKeys.MINOR] = properties.getProperty(
                PropertyNames.VERSION_MINOR,
                properties.getProperty(PropertyNames.VERSION_MINOR_ALT, DEFAULT_MINOR)
            )
            versionMap[VersionKeys.PATCH] = properties.getProperty(
                PropertyNames.VERSION_PATCH,
                properties.getProperty(PropertyNames.VERSION_PATCH_ALT, DEFAULT_PATCH)
            )
            versionMap[VersionKeys.BUILD] = properties.getProperty(
                PropertyNames.VERSION_BUILD,
                properties.getProperty(PropertyNames.VERSION_BUILD_ALT, DEFAULT_BUILD)
            )

            Log.d(TAG, "Version components: $versionMap")

        } catch (e: IOException) {
            Log.e(TAG, "Failed to read version properties", e)
        }

        return versionMap
    }

    /**
     * Gets all version properties from the properties file
     *
     * @param context The application context
     * @return Map containing all version properties or empty map if reading fails
     */
    fun getAllVersionProperties(context: Context): Map<String, String> {
        return try {
            Properties().apply {
                context.assets.open(VERSION_PROPERTIES_FILE).use { load(it) }
            }.stringPropertyNames().associateWith { getProperty(it) ?: "" }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read version properties", e)
            emptyMap()
        }
    }
}