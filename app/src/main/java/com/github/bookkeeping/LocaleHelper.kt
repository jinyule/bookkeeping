package com.github.bookkeeping

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LanguageMode {
    const val SYSTEM = "system"
    const val ZH = "zh"
    const val EN = "en"
}

object LocaleHelper {
    fun wrap(context: Context): Context {
        val locale = resolveLocale(context, SettingsStore(context).languageMode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun resolveLocale(context: Context, mode: String): Locale {
        return when (mode) {
            LanguageMode.EN -> Locale.ENGLISH
            LanguageMode.ZH -> Locale.SIMPLIFIED_CHINESE
            else -> {
                val systemLocale = context.resources.configuration.locales.get(0)
                if (systemLocale.language.equals("en", ignoreCase = true)) {
                    Locale.ENGLISH
                } else {
                    Locale.SIMPLIFIED_CHINESE
                }
            }
        }
    }
}
