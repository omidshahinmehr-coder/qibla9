package com.qibla.prayertimes.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Supported in-app language override. "system" means "follow the device language" (the
 * default) — the other three force the app into that language regardless of device settings.
 */
object LocalePrefs {
    private const val PREFS_NAME = "qibla_locale_prefs"
    private const val KEY_LANGUAGE = "language_override"
    const val SYSTEM = "system"

    val SUPPORTED = listOf(SYSTEM, "en", "fa", "ar")

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, SYSTEM) ?: SYSTEM

    fun set(context: Context, language: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language).apply()
    }

    /**
     * Wraps [base] with the stored language override applied to its configuration, if any.
     * Called from [android.app.Activity.attachBaseContext] so every screen — including the
     * very first frame — already sees the right language, not just after a recreate.
     */
    fun wrap(base: Context): Context {
        val language = get(base)
        if (language == SYSTEM) return base
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }
}
