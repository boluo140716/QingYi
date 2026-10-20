package com.qingyi.translate.ui

import android.net.Uri
import com.qingyi.translate.data.DeepSeekSettings
import com.qingyi.translate.data.Language

enum class Screen {
    TRANSLATE,
    CAMERA,
    IMAGE,
    SETTINGS,
}

sealed interface TranslationUi {
    data object Idle : TranslationUi
    data object Loading : TranslationUi
    data class Success(val text: String) : TranslationUi
    data class Failure(val message: String) : TranslationUi
    data object NeedKey : TranslationUi
}

/** State of the image editor / vision translation flow. */
sealed interface ImageUi {
    data object None : ImageUi
    data class Editing(val uri: Uri) : ImageUi
    data class Translating(val uri: Uri) : ImageUi
    data class Success(val uri: Uri, val translated: String) : ImageUi
    data class Failure(val uri: Uri, val message: String) : ImageUi
    data class NeedKey(val uri: Uri) : ImageUi
}

val ImageUi.uriOrNull: Uri?
    get() = when (this) {
        ImageUi.None -> null
        is ImageUi.Editing -> uri
        is ImageUi.Translating -> uri
        is ImageUi.Success -> uri
        is ImageUi.Failure -> uri
        is ImageUi.NeedKey -> uri
    }

data class LanguagePickerState(val forSource: Boolean)

enum class AssistantRole { USER, ASSISTANT }

data class AssistantTurn(val role: AssistantRole, val text: String)

/**
 * The follow-up conversation with the model. The first translation is not part
 * of [turns]; it stays in [TranslationUi] / [ImageUi] and is sent as context.
 */
data class AssistantUi(
    val open: Boolean = false,
    val turns: List<AssistantTurn> = emptyList(),
    val input: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

data class AppUiState(
    val screen: Screen = Screen.TRANSLATE,
    val inputText: String = "",
    val source: Language? = null,
    val target: Language = Language.CHINESE_SIMPLIFIED,
    val translation: TranslationUi = TranslationUi.Idle,
    val image: ImageUi = ImageUi.None,
    val assistant: AssistantUi = AssistantUi(),
    val settings: DeepSeekSettings = DeepSeekSettings(),
    val languagePicker: LanguagePickerState? = null,
)
