package com.mentality.sonethyst

import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object AppLocale {
    const val SYSTEM = "system"
    const val ENGLISH = "en"
    const val RUSSIAN = "ru"
    const val UKRAINIAN = "uk"
    const val FRENCH = "fr"

    private const val PREFS = "sonethyst_locale"
    private const val KEY = "language"
    private val supported = setOf(SYSTEM, ENGLISH, RUSSIAN, UKRAINIAN, FRENCH)

    fun normalize(code: String?): String = code?.takeIf { it in supported } ?: SYSTEM

    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= 33) {
            val tags = context.getSystemService(LocaleManager::class.java)
                ?.applicationLocales?.toLanguageTags().orEmpty()
            return normalize(tags.substringBefore('-').substringBefore(','))
        }
        return normalize(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, SYSTEM))
    }

    fun apply(context: Context, code: String) {
        val normalized = normalize(code)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, normalized).apply()
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = if (normalized == SYSTEM) android.os.LocaleList.getEmptyLocaleList()
            else android.os.LocaleList.forLanguageTags(normalized)
            context.getSystemService(LocaleManager::class.java)?.applicationLocales = locales
        }
    }

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context
        val code = current(context)
        if (code == SYSTEM) return context
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(code))
        return ContextWrapper(context.createConfigurationContext(config))
    }
}
