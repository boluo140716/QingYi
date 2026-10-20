package com.qingyi.translate.data

/** Everything needed to talk to a DeepSeek (OpenAI-compatible) endpoint. */
data class DeepSeekSettings(
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val model: String = "deepseek-flash",
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
}

sealed interface TranslateResult {
    data class Success(val text: String) : TranslateResult
    data class Error(val message: String) : TranslateResult
}
