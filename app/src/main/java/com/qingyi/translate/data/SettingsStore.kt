package com.qingyi.translate.data

import android.content.Context

/** Tiny SharedPreferences wrapper. The key never leaves the device. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): DeepSeekSettings = DeepSeekSettings(
        apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
        baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL).orEmpty(),
        model = prefs.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty(),
    )

    /** Null means "never chosen", so the caller can fall back to the device locale. */
    fun loadTargetLanguage(): Language? =
        prefs.getString(KEY_TARGET_LANGUAGE, null)
            ?.let { name -> runCatching { Language.valueOf(name) }.getOrNull() }

    fun saveTargetLanguage(language: Language) {
        prefs.edit().putString(KEY_TARGET_LANGUAGE, language.name).apply()
    }

    fun save(settings: DeepSeekSettings) {
        prefs.edit()
            .putString(KEY_API_KEY, settings.apiKey.trim())
            .putString(KEY_BASE_URL, settings.baseUrl.trim())
            .putString(KEY_MODEL, settings.model.trim())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "qingyi_settings"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_TARGET_LANGUAGE = "target_language"

        const val DEFAULT_BASE_URL = "https://api.deepseek.com/v1"
        const val DEFAULT_MODEL = "deepseek-flash"
    }
}
