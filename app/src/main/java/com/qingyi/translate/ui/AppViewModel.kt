package com.qingyi.translate.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qingyi.translate.data.ChatTurn
import com.qingyi.translate.data.DeepSeekClient
import com.qingyi.translate.data.DeepSeekSettings
import com.qingyi.translate.data.Language
import com.qingyi.translate.data.LanguageTag
import com.qingyi.translate.data.SettingsStore
import com.qingyi.translate.data.TranslateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore = SettingsStore(application)
    private val client = DeepSeekClient()

    var state by mutableStateOf(
        AppUiState(
            settings = settingsStore.load(),
            target = settingsStore.loadTargetLanguage()
                ?: Language.forLocale(Locale.getDefault()),
        )
    )
        private set

    private var translationJob: Job? = null
    private var imageJob: Job? = null
    private var assistantJob: Job? = null
    private var lastCrop: Bitmap? = null
    private var lastFull: Bitmap? = null
    private var lastImageBase64: String? = null
    private var lastContextBase64: String? = null

    val canRetryImage: Boolean get() = lastCrop != null

    // ---------------------------------------------------------------- text

    /**
     * Typing never triggers a request. It only invalidates the previous result;
     * the user presses the translate button when they are ready.
     */
    fun onInputChange(text: String) {
        if (text == state.inputText) return
        // Editing invalidates both the result and the in-flight requests, otherwise
        // a late response lands on the new text/language and looks mismatched.
        translationJob?.cancel()
        assistantJob?.cancel()
        state = state.copy(
            inputText = text,
            translation = TranslationUi.Idle,
            detectedLanguage = null,
            assistant = AssistantUi(),
        )
    }

    fun clearInput() {
        translationJob?.cancel()
        state = state.copy(
            inputText = "",
            translation = TranslationUi.Idle,
            detectedLanguage = null,
            assistant = AssistantUi(),
        )
    }

    fun translateNow() {
        translationJob?.cancel()
        val text = state.inputText.trim()
        if (text.isEmpty()) return
        if (!state.settings.isConfigured) {
            state = state.copy(translation = TranslationUi.NeedKey)
            return
        }

        state = state.copy(
            translation = TranslationUi.Loading,
            detectedLanguage = null,
            assistant = AssistantUi(),
        )
        translationJob = viewModelScope.launch {
            var lastEmit = 0L
            val result = client.translateText(
                settings = state.settings,
                text = text,
                sourceName = state.source?.promptName,
                targetName = state.target.promptName,
                onDelta = { partial ->
                    // Repaint at most every ~60 ms: smooth enough to read, cheap
                    // enough to keep recomposition off the streaming hot path.
                    val now = System.nanoTime()
                    if (now - lastEmit >= 60_000_000L) {
                        lastEmit = now
                        withContext(Dispatchers.Main) {
                            val visible = LanguageTag.visible(partial)
                            val detected = LanguageTag.split(partial).first
                            state = state.copy(
                                translation = TranslationUi.Streaming(visible),
                                detectedLanguage = detected ?: state.detectedLanguage,
                            )
                        }
                    }
                },
            )
            if (!isActive) return@launch
            state = when (result) {
                is TranslateResult.Success -> {
                    val (detected, cleanText) = LanguageTag.split(result.text)
                    state.copy(
                        translation = TranslationUi.Success(cleanText),
                        detectedLanguage = detected ?: state.detectedLanguage,
                    )
                }

                is TranslateResult.Error -> state.copy(
                    translation = TranslationUi.Failure(result.message)
                )
            }
        }
    }

    /** Stops the in-flight text request and keeps whatever text already arrived. */
    fun cancelTranslation() {
        translationJob?.cancel()
        val partial = (state.translation as? TranslationUi.Streaming)?.text
        state = state.copy(
            translation = if (partial.isNullOrBlank()) {
                TranslationUi.Idle
            } else {
                TranslationUi.Success(partial)
            }
        )
    }

    fun retryTextTranslation() = translateNow()

    // ------------------------------------------------------------ languages

    fun openLanguagePicker(forSource: Boolean) {
        state = state.copy(languagePicker = LanguagePickerState(forSource))
    }

    fun closeLanguagePicker() {
        state = state.copy(languagePicker = null)
    }

    fun selectLanguage(language: Language?) {
        val picker = state.languagePicker ?: return
        translationJob?.cancel()
        assistantJob?.cancel()
        state = if (picker.forSource) {
            state.copy(
                source = language,
                languagePicker = null,
                translation = TranslationUi.Idle,
                detectedLanguage = null,
                assistant = AssistantUi(),
            )
        } else {
            val newTarget = language ?: state.target
            settingsStore.saveTargetLanguage(newTarget)
            state.copy(
                target = newTarget,
                languagePicker = null,
                translation = TranslationUi.Idle,
                detectedLanguage = null,
                assistant = AssistantUi(),
            )
        }
    }

    fun swapLanguages() {
        translationJob?.cancel()
        assistantJob?.cancel()
        val oldSource = state.source
        val oldTarget = state.target
        val newSource = oldTarget
        val newTarget = oldSource ?: if (oldTarget == Language.CHINESE_SIMPLIFIED) {
            Language.ENGLISH
        } else {
            Language.CHINESE_SIMPLIFIED
        }
        val swappedText = (state.translation as? TranslationUi.Success)?.text
        settingsStore.saveTargetLanguage(newTarget)
        state = state.copy(
            source = newSource,
            target = newTarget,
            inputText = swappedText ?: state.inputText,
            translation = TranslationUi.Idle,
            detectedLanguage = null,
            assistant = AssistantUi(),
        )
    }

    // ------------------------------------------------------------- screens

    fun go(screen: Screen) {
        state = state.copy(screen = screen, languagePicker = null)
    }

    // ------------------------------------------------------------- images

    fun onImagePicked(uri: Uri) {
        imageJob?.cancel()
        lastCrop = null
        lastFull = null
        lastImageBase64 = null
        lastContextBase64 = null
        state = state.copy(
            screen = Screen.IMAGE,
            image = ImageUi.Editing(uri),
            assistant = AssistantUi(),
        )
    }

    /**
     * [full] is the whole photo, [crop] is the selected region. The full frame
     * is sent as visual context so the model can understand the scene before
     * translating. When the crop covers the whole photo we skip the duplicate.
     */
    fun translateCrop(full: Bitmap, crop: Bitmap) {
        val uri = state.image.uriOrNull ?: return
        lastCrop = crop
        lastFull = full

        if (!state.settings.isConfigured) {
            state = state.copy(image = ImageUi.NeedKey(uri))
            return
        }

        imageJob?.cancel()
        state = state.copy(
            image = ImageUi.Translating(uri),
            assistant = AssistantUi(),
        )
        imageJob = viewModelScope.launch {
            // A full-frame selection needs no context image: the target *is* the
            // scene, and the model finds the text itself.
            val isFullSelection = crop.width >= full.width - 1 && crop.height >= full.height - 1
            val targetBitmap = if (isFullSelection) full else crop

            // A crop covering most of the frame already carries the scene; a second
            // copy would only double the upload for no extra context.
            val cropArea = crop.width.toLong() * crop.height
            val fullArea = full.width.toLong() * full.height
            val needsContext = !isFullSelection && cropArea * 100 < fullArea * 75

            val targetBase64 = withContext(Dispatchers.Default) {
                encodeJpeg(targetBitmap, maxSide = 1600, quality = 85)
            }
            val contextBase64 = withContext(Dispatchers.Default) {
                if (needsContext) encodeJpeg(full, maxSide = 1024, quality = 75) else null
            }
            lastImageBase64 = targetBase64
            lastContextBase64 = contextBase64

            var lastEmit = 0L
            val result = client.translateImage(
                settings = state.settings,
                jpegBase64 = targetBase64,
                contextJpegBase64 = contextBase64,
                sourceName = state.source?.promptName,
                targetName = state.target.promptName,
                onDelta = { partial ->
                    val now = System.nanoTime()
                    if (now - lastEmit >= 60_000_000L) {
                        lastEmit = now
                        withContext(Dispatchers.Main) {
                            state = state.copy(
                                image = ImageUi.Streaming(uri, LanguageTag.visible(partial))
                            )
                        }
                    }
                },
            )
            if (!isActive) return@launch

            state = state.copy(
                image = when (result) {
                    // The model may prefix a `[[语言]]` marker for auto-detect;
                    // it is metadata, not part of the overlay text.
                    is TranslateResult.Success ->
                        ImageUi.Success(uri, LanguageTag.split(result.text).second)
                    is TranslateResult.Error -> ImageUi.Failure(uri, result.message)
                }
            )
        }
    }

    /** Stops the in-flight image request and keeps whatever text already arrived. */
    fun cancelImageTranslation() {
        imageJob?.cancel()
        val uri = state.image.uriOrNull ?: return
        val partial = (state.image as? ImageUi.Streaming)?.translated
        state = state.copy(
            image = if (partial.isNullOrBlank()) {
                ImageUi.Editing(uri)
            } else {
                ImageUi.Success(uri, partial)
            }
        )
    }

    fun retryImage() {
        val full = lastFull ?: return
        val crop = lastCrop ?: return
        translateCrop(full, crop)
    }

    /** Closes the result panel but keeps the selected image, ready for another crop. */
    fun dismissImageResult() {
        val uri = state.image.uriOrNull ?: return
        if (state.image is ImageUi.Success ||
            state.image is ImageUi.Failure ||
            state.image is ImageUi.NeedKey
        ) {
            state = state.copy(image = ImageUi.Editing(uri))
        }
    }

    private fun encodeJpeg(bitmap: Bitmap, maxSide: Int, quality: Int): String {
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longest > maxSide) {
            val ratio = maxSide.toFloat() / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            bitmap
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    // ------------------------------------------------------------ assistant

    fun openAssistant(prompt: String? = null) {
        val hasContext = when (state.screen) {
            Screen.IMAGE -> state.image is ImageUi.Success
            else -> state.translation is TranslationUi.Success
        }
        if (!hasContext) return
        state = state.copy(assistant = state.assistant.copy(open = true, error = null))
        if (!prompt.isNullOrBlank()) ask(prompt)
    }

    fun closeAssistant() {
        state = state.copy(assistant = state.assistant.copy(open = false))
    }

    fun onAssistantInputChange(text: String) {
        state = state.copy(assistant = state.assistant.copy(input = text))
    }

    fun sendAssistantQuestion() {
        ask(state.assistant.input)
    }

    private fun ask(question: String) {
        val q = question.trim()
        if (q.isEmpty() || state.assistant.loading) return

        val useImageContext = state.screen == Screen.IMAGE
        val imageSuccess = if (useImageContext) state.image as? ImageUi.Success else null
        val textSuccess = if (!useImageContext) state.translation as? TranslationUi.Success else null
        if (imageSuccess == null && textSuccess == null) return

        if (!state.settings.isConfigured) {
            state = state.copy(
                assistant = state.assistant.copy(error = "请先在设置里填写 DeepSeek API Key")
            )
            return
        }

        val turns = (state.assistant.turns + AssistantTurn(AssistantRole.USER, q)).takeLast(12)
        val target = state.target
        val sourceText = state.inputText
        val isFirstImageTurn = state.assistant.turns.isEmpty()

        val streamingTurn = AssistantTurn(AssistantRole.ASSISTANT, "")
        state = state.copy(
            assistant = state.assistant.copy(
                open = true,
                turns = turns + streamingTurn,
                input = "",
                loading = true,
                error = null,
            )
        )

        assistantJob?.cancel()
        assistantJob = viewModelScope.launch {
            val history = turns.map { turn ->
                ChatTurn(
                    role = if (turn.role == AssistantRole.USER) ChatTurn.USER else ChatTurn.ASSISTANT,
                    content = turn.text,
                )
            }

            // Same 60 ms repaint budget as translation: the bubble follows the
            // stream without recomposing on every token.
            var lastEmit = 0L
            val onDelta: suspend (String) -> Unit = { partial ->
                val now = System.nanoTime()
                if (now - lastEmit >= 60_000_000L) {
                    lastEmit = now
                    withContext(Dispatchers.Main) {
                        state = state.copy(
                            assistant = state.assistant.copy(
                                turns = turns + streamingTurn.copy(text = partial)
                            )
                        )
                    }
                }
            }

            val result = when {
                imageSuccess != null && lastImageBase64 != null -> client.askAboutImage(
                    settings = state.settings,
                    jpegBase64 = lastImageBase64!!,
                    contextJpegBase64 = lastContextBase64,
                    translation = imageSuccess.translated,
                    targetName = target.promptName,
                    turns = history,
                    includeImage = isFirstImageTurn,
                    onDelta = onDelta,
                )

                textSuccess != null -> client.askAboutText(
                    settings = state.settings,
                    original = sourceText,
                    translation = textSuccess.text,
                    targetName = target.promptName,
                    turns = history,
                    onDelta = onDelta,
                )

                else -> TranslateResult.Error("当前没有可用的翻译内容")
            }
            if (!isActive) return@launch

            state = when (result) {
                is TranslateResult.Success -> state.copy(
                    assistant = state.assistant.copy(
                        turns = turns + streamingTurn.copy(text = result.text),
                        loading = false,
                        error = null,
                    )
                )

                is TranslateResult.Error -> {
                    // Keep whatever already streamed in; dropping it would punish
                    // the user for a timeout that arrived after a usable answer.
                    val partial = state.assistant.turns.lastOrNull()?.text.orEmpty()
                    if (partial.isNotBlank()) {
                        state.copy(
                            assistant = state.assistant.copy(
                                loading = false,
                                error = result.message,
                            )
                        )
                    } else {
                        state.copy(
                            assistant = state.assistant.copy(
                                turns = turns.dropLast(1),
                                input = q,
                                loading = false,
                                error = result.message,
                            )
                        )
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ settings

    fun saveSettings(apiKey: String, baseUrl: String, model: String) {
        val settings = DeepSeekSettings(
            apiKey = apiKey.trim(),
            baseUrl = baseUrl.trim().ifBlank { SettingsStore.DEFAULT_BASE_URL },
            model = model.trim().ifBlank { SettingsStore.DEFAULT_MODEL },
        )
        settingsStore.save(settings)

        val target = when {
            state.screen == Screen.IMAGE && state.image != ImageUi.None -> Screen.IMAGE
            else -> Screen.TRANSLATE
        }
        // Saving a key resolves the "needs key" placeholders. Without this the user
        // returns to a card that still claims no key is configured.
        val translation = if (state.translation is TranslationUi.NeedKey) {
            TranslationUi.Idle
        } else {
            state.translation
        }
        val image = when (val current = state.image) {
            is ImageUi.NeedKey -> ImageUi.Editing(current.uri)
            else -> current
        }
        state = state.copy(
            settings = settings,
            screen = target,
            languagePicker = null,
            translation = translation,
            image = image,
        )
    }
}
