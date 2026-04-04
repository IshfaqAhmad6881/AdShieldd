package com.FusionCoreTech.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.FusionCoreTech.myapplication.localization.AppLanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { System, Light, Dark }

private const val PREFS_NAME = "adshield_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_LANGUAGE_TAG = "app_language_tag"

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private val _languageTag = MutableStateFlow(loadLanguageTag())
    val languageTag: StateFlow<String> = _languageTag.asStateFlow()

    /** Resolved dark mode for UI: when System, caller must pass system value; otherwise Light=false, Dark=true. */
    fun isDarkMode(systemInDarkTheme: Boolean): Boolean = when (_themeMode.value) {
        ThemeMode.System -> systemInDarkTheme
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun setLanguageTag(tag: String) {
        val normalized = AppLanguageManager.normalizeTag(tag)
        _languageTag.value = normalized
        prefs.edit().putString(KEY_LANGUAGE_TAG, normalized).apply()
        AppLanguageManager.applyLanguage(normalized)
    }

    private fun loadThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.System.name) ?: ThemeMode.System.name
        return try {
            ThemeMode.valueOf(name)
        } catch (_: Exception) {
            ThemeMode.System
        }
    }

    private fun loadLanguageTag(): String {
        val saved = prefs.getString(KEY_LANGUAGE_TAG, AppLanguageManager.SYSTEM_LANGUAGE_TAG)
        return AppLanguageManager.normalizeTag(saved)
    }
}
