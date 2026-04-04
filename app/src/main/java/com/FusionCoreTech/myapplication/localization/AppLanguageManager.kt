package com.FusionCoreTech.myapplication.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

data class AppLanguage(
    val tag: String,
    val name: String
)

object AppLanguageManager {
    const val SYSTEM_LANGUAGE_TAG = "system"

    val supportedLanguages = listOf(
        AppLanguage(SYSTEM_LANGUAGE_TAG, "System Default"),
        AppLanguage("en", "English"),
        AppLanguage("fr", "Français"),
        AppLanguage("de", "Deutsch"),
        AppLanguage("nl", "Nederlands"),
        AppLanguage("es", "Español"),
        AppLanguage("pt", "Português"),
        AppLanguage("it", "Italiano"),
        AppLanguage("ru", "Русский"),
        AppLanguage("ar", "العربية"),
        AppLanguage("hi", "हिन्दी"),
        AppLanguage("ur", "اردو"),
        AppLanguage("tr", "Türkçe"),
        AppLanguage("zh-CN", "简体中文"),
        AppLanguage("ja", "日本語"),
        AppLanguage("ko", "한국어"),
        AppLanguage("id", "Bahasa Indonesia"),
        AppLanguage("vi", "Tiếng Việt"),
        AppLanguage("bn", "বাংলা"),
        AppLanguage("fa", "فارسی"),
        AppLanguage("pl", "Polski")
    )

    fun normalizeTag(tag: String?): String {
        val clean = tag?.trim().orEmpty()
        return if (supportedLanguages.any { it.tag == clean }) clean else SYSTEM_LANGUAGE_TAG
    }

    /**
     * Apply app locale. Must be called before [android.app.Activity.onCreate] super where possible
     * so [android.content.res.Configuration] and resources resolve correctly.
     * System default uses [LocaleListCompat.getEmptyLocaleList]; empty [LocaleListCompat.forLanguageTags] does not reset reliably.
     */
    fun applyLanguage(tag: String) {
        val normalized = normalizeTag(tag)
        val list = if (normalized == SYSTEM_LANGUAGE_TAG) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(normalized)
        }
        AppCompatDelegate.setApplicationLocales(list)
    }
}
