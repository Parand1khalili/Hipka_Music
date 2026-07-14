package com.hipka.app.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {

    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_PERSIAN = "fa"

    fun setLocale(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }

    fun currentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) LANGUAGE_ENGLISH else locales[0]?.language ?: LANGUAGE_ENGLISH
    }
}