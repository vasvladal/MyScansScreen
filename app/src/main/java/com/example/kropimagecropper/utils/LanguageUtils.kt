package com.example.kropimagecropper.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import java.util.*

object LanguageUtils {
    private const val PREFS_NAME = "KropImageCropperPrefs"
    private const val LANGUAGE_KEY = "app_language"

    /**
     * Changes the app language and persists the selection
     */
    fun setAppLanguage(context: Context, languageCode: String) {
        persistLanguageSelection(context, languageCode)
        updateResources(context, languageCode)
    }

    /**
     * Gets the currently selected language from SharedPreferences
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(LANGUAGE_KEY, getSystemLanguage()) ?: getSystemLanguage()
    }

    /**
     * Applies the saved language when the app starts
     */
    fun applySavedLanguage(context: Context) {
        val languageCode = getCurrentLanguage(context)
        updateResources(context, languageCode)
    }

    /**
     * Gets available supported languages
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("en", "ru", "uk", "ro") // English, Russian, Ukrainian, Romanian
    }

    /**
     * Gets display names for supported languages
     */
    fun getLanguageDisplayNames(): Map<String, String> {
        return mapOf(
            "en" to "English",
            "ru" to "Русский",
            "uk" to "Українська",
            "ro" to "Română"
        )
    }

    private fun persistLanguageSelection(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, languageCode)
            .apply()
    }

    private fun getSystemLanguage(): String {
        val systemLang = Locale.getDefault().language
        return if (getSupportedLanguages().contains(systemLang)) systemLang else "en"
    }

    private fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            configuration.setLayoutDirection(locale)
        } else {
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Update night mode when language changes (for RTL support)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    /**
     * Creates context with updated configuration for the new language
     */
    fun createLanguageContext(context: Context): Context {
        val languageCode = getCurrentLanguage(context)
        val locale = Locale(languageCode)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val configuration = context.resources.configuration
            configuration.setLocale(locale)
            context.createConfigurationContext(configuration)
        } else {
            context
        }
    }
}