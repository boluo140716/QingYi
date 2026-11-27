package com.qingyi.translate.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Minimal DeepSeek client for the `deepseek-flash` model (V4.1 Flash).
 *
 * Text uses a system + user message pair; images use the OpenAI-compatible
 * multimodal content array with an inline base64 image. One POST is all we
 * need, so HttpURLConnection keeps the app free of networking frameworks.
 */
class DeepSeekClient {

    /**
     * Set when a custom endpoint answers `stream = true` with plain JSON. Later
     * calls then skip SSE so translation keeps working instead of coming back empty.
     */
    private var streamingUnsupported = false

    suspend fun translateText(
        settings: DeepSeekSettings,
        text: String,
        sourceName: String?,
        targetName: String,
        onDelta: suspend (String) -> Unit = {},
    ): TranslateResult = withContext(Dispatchers.IO) {
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", textSystemPrompt(sourceName, targetName))
            )
            .put(JSONObject().put("role", "user").put("content", text))

        executeStream(settings, messages, onDelta)
    }

    /**
     * Sends the selected region to the vision model. When [contextJpegBase64]
     * is present it is sent first as a full-frame context image, so the model
     * can understand the scene before translating the crop.
     */
    suspend fun translateImage(
        settings: DeepSeekSettings,
        jpegBase64: String,
        contextJpegBase64: String?,
        sourceName: String?,
        targetName: String,
        onDelta: suspend (String) -> Unit = {},
    ): TranslateResult = withContext(Dispatchers.IO) {
        val content = JSONArray()
            .put(
                JSONObject()
                    .put("type", "text")
                    .put("text", imagePrompt(sourceName, targetName, contextJpegBase64 != null))
            )

        if (contextJpegBase64 != null) {
            // The context frame only has to convey the scene, so it skips the
            // `detail` hint and is uploaded much smaller by the caller.
            content.put(imageBlock(contextJpegBase64, detail = null))
        }
        content.put(imageBlock(jpegBase64))

        val messages = JSONArray().put(
            JSONObject().put("role", "user").put("content", content)
        )

        executeStream(settings, messages, onDelta)
    }

    private fun imageBlock(base64: String, detail: String? = "original"): JSONObject {
        val imageUrl = JSONObject()
            .put("url", "data:image/jpeg;base64,$base64")
        if (detail != null) imageUrl.put("detail", detail)
        return JSONObject()
            .put("type", "image_url")
            .put("image_url", imageUrl)
    }

    /**
     * Follow-up questions about a text translation. The original text and the
     * translation are injected as context so the model can explain choices.
     */
    suspend fun askAboutText(
        settings: DeepSeekSettings,
        original: String,
        translation: String,
        targetName: String,
        turns: List<ChatTurn>,
        onDelta: suspend (String) -> Unit = {},
    ): TranslateResult = withContext(Dispatchers.IO) {
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put(
                        "content",
                        assistantPrompt(targetName) +
                            "\n\n用户的原文：\n$original\n\n已经给出的译文：\n$translation"
                    )
            )

        turns.forEach { turn ->
            messages.put(
                JSONObject().put("role", turn.role).put("content", turn.content)
            )
        }

        executeStream(settings, messages, onDelta)
    }

    /**
     * Follow-up questions about an image.
     *
     * [includeImage] is true only for the first question: the image is sent once
     * (~1024 vision tokens), and later turns rely on the translation plus the
     * conversation transcript, which keeps follow-ups cheap.
     */
    suspend fun askAboutImage(
        settings: DeepSeekSettings,
        jpegBase64: String,
        contextJpegBase64: String?,
        translation: String,
        targetName: String,
        turns: List<ChatTurn>,
        includeImage: Boolean,
        onDelta: suspend (String) -> Unit = {},
    ): TranslateResult = withContext(Dispatchers.IO) {
        val messages = JSONArray()
            .put(
                JSONObject()
                    .put("role", "system")
                    .put("content", assistantPrompt(targetName))
            )

        if (includeImage) {
            val intro = buildString {
                if (contextJpegBase64 != null) {
                    append("第一张是完整画面，第二张是用户选中的区域。")
                }
                append("图中文字已经翻译成")
                append(targetName)
                append("，译文如下：\n")
                append(translation)
                append("\n\n接下来用户会针对图片或译文提问。")
            }

            val imageContent = JSONArray()
                .put(JSONObject().put("type", "text").put("text", intro))

            if (contextJpegBase64 != null) {
                imageContent.put(imageBlock(contextJpegBase64))
            }
            imageContent.put(imageBlock(jpegBase64))

            messages
                .put(JSONObject().put("role", "user").put("content", imageContent))
                .put(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", "好的，我已经看到图片和译文。")
                )
        } else {
            val context = buildString {
                append("这是之前那张图片中文字的译文：\n")
                append(translation)
                append("\n\n图片不再重复发送。请基于这段译文和之前的对话继续回答。")
            }
            messages
                .put(JSONObject().put("role", "user").put("content", context))
                .put(
                    JSONObject()
                        .put("role", "assistant")
                        .put("content", "好的，我记住了这张图片的译文。")
                )
        }

        turns.forEach { turn ->
            messages.put(
                JSONObject().put("role", turn.role).put("content", turn.content)
            )
        }

        executeStream(settings, messages, onDelta)
    }

    private fun assistantPrompt(targetName: String): String = buildString {
        append("你是翻译助手，当前目标语言是")
        append(targetName)
        append("。请结合原文和译文回答用户的问题，要求：\n")
        append("1. 简洁、准确，优先解决用户真正想问的点。\n")
        append("2. 用户问到含义、语气、文化背景时，给出必要解释。\n")
        append("3. 用户想改译法时，直接给出更好的译法，并简单说明差别。\n")
        append("4. 不要重复整段译文，除非用户要求。")
    }

    private fun execute(settings: DeepSeekSettings, messages: JSONArray): TranslateResult {
        if (settings.apiKey.isBlank()) {
            return TranslateResult.Error("请先在设置里填写 DeepSeek API Key")
        }

        val endpoint = endpointFor(settings.baseUrl)
        val payload = JSONObject()
            .put("model", settings.model.ifBlank { SettingsStore.DEFAULT_MODEL })
            .put("temperature", 0.2)
            .put("stream", false)
            .put("messages", messages)
            .toString()

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 90_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer ${settings.apiKey.trim()}")
            }

            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                TranslateResult.Error(httpErrorMessage(code, raw))
            } else {
                val text = JSONObject(raw)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()

                if (text.isNullOrBlank()) {
                    TranslateResult.Error("翻译服务返回了空内容，请重试")
                } else {
                    TranslateResult.Success(text)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            TranslateResult.Error("网络不可用，请检查网络连接")
        } catch (e: SocketTimeoutException) {
            TranslateResult.Error("请求超时，请稍后重试")
        } catch (e: IOException) {
            TranslateResult.Error("网络请求失败：${e.message ?: "未知错误"}")
        } catch (e: Exception) {
            TranslateResult.Error("翻译失败：${e.message ?: "未知错误"}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Same request as [execute], but asks the API for SSE and reports the
     * accumulated text after every chunk. The first characters land in a few
     * hundred milliseconds, which is what makes translation feel instant.
     */
    private suspend fun executeStream(
        settings: DeepSeekSettings,
        messages: JSONArray,
        onDelta: suspend (String) -> Unit,
    ): TranslateResult {
        if (settings.apiKey.isBlank()) {
            return TranslateResult.Error("请先在设置里填写 DeepSeek API Key")
        }
        if (streamingUnsupported) {
            return execute(settings, messages)
        }

        val endpoint = endpointFor(settings.baseUrl)
        val payload = JSONObject()
            .put("model", settings.model.ifBlank { SettingsStore.DEFAULT_MODEL })
            .put("temperature", 0.2)
            .put("stream", true)
            .put("messages", messages)
            .toString()
        val body = payload.toByteArray(Charsets.UTF_8)

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 90_000
                doOutput = true
                setFixedLengthStreamingMode(body.size)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Authorization", "Bearer ${settings.apiKey.trim()}")
            }

            connection.outputStream.use { stream ->
                stream.write(body)
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                val raw = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                return TranslateResult.Error(httpErrorMessage(code, raw))
            }

            val accumulated = StringBuilder()
            var sawData = false
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (!line.startsWith("data:")) continue
                    val data = line.removePrefix("data:").trim()
                    if (data.isEmpty()) continue
                    sawData = true
                    if (data == "[DONE]") break

                    val delta = runCatching {
                        JSONObject(data)
                            .optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                            ?.optString("content")
                    }.getOrNull()

                    if (!delta.isNullOrEmpty()) {
                        accumulated.append(delta)
                        onDelta(accumulated.toString())
                    }
                }
            }

            // A gateway that ignored `stream` returned plain JSON, which the SSE
            // parser skips entirely. Retry once without streaming and remember it.
            if (!sawData && accumulated.isEmpty()) {
                streamingUnsupported = true
                return execute(settings, messages)
            }

            val text = accumulated.toString().trim()
            if (text.isEmpty()) {
                TranslateResult.Error("翻译服务返回了空内容，请重试")
            } else {
                TranslateResult.Success(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            TranslateResult.Error("网络不可用，请检查网络连接")
        } catch (e: SocketTimeoutException) {
            TranslateResult.Error("请求超时，请稍后重试")
        } catch (e: IOException) {
            TranslateResult.Error("网络请求失败：${e.message ?: "未知错误"}")
        } catch (e: Exception) {
            TranslateResult.Error("翻译失败：${e.message ?: "未知错误"}")
        } finally {
            connection?.disconnect()
        }
    }

    private fun textSystemPrompt(sourceName: String?, targetName: String): String = buildString {
        append("你是专业的翻译引擎。请把用户提供的文本翻译成")
        append(targetName)
        append("。\n规则：\n")
        append("1. ")
        if (sourceName == null) append("除语言标记那一行外，")
        append("只输出译文，不要解释，不要加引号，不要附原文。\n")
        append("2. 保留原文的段落和换行结构。\n")
        append("3. 如果原文已经是")
        append(targetName)
        append("，原样返回。\n")
        if (sourceName == null) {
            append("4. 请自动判断源语言。\n")
            append("5. 在译文之前单独输出一行 [[源语言的中文名称]]，例如 [[英语]]，然后从下一行开始输出译文。")
        } else {
            append("4. 源语言是")
            append(sourceName)
            append("。")
        }
    }

    private fun imagePrompt(
        sourceName: String?,
        targetName: String,
        hasContext: Boolean,
    ): String = buildString {
        if (hasContext) {
            append("你会收到两张图片：第一张是完整画面，用来理解场景；第二张是用户框选的区域，需要翻译。\n")
            append("请先结合完整画面判断场景、文字用途和语气，再翻译第二张图中的文字。\n")
        } else {
            append("你会收到一张图片。请先理解画面场景、文字用途和语气，找出画面中真正需要翻译的文字")
            append("（忽略水印、状态栏、装饰性文字等次要内容），再翻译。\n")
        }

        append("翻译成")
        append(targetName)
        append("。\n要求：\n")
        append("1. 结合画面语境选择词义和语气，避免脱离场景的逐字直译。\n")
        if (hasContext) {
            append("2. 只翻译第二张图中的文字，只输出译文，不要解释画面，不要加引号，不要附原文。\n")
        } else {
            append("2. 只输出译文，不要解释画面，不要加引号，不要附原文。\n")
        }
        append("3. 保留图片中原有的分行和段落。\n")
        append("4. 如果图片里没有可翻译的文字，只输出：未识别到文字。\n")

        if (sourceName == null) {
            append("5. 请自动判断源语言。\n")
            append("6. 在译文之前单独输出一行 [[源语言的中文名称]]，例如 [[英语]]，然后从下一行开始输出译文。")
        } else {
            append("5. 源语言是")
            append(sourceName)
            append("。")
        }
    }

    /** Accepts https://api.deepseek.com, .../v1 or a full .../chat/completions URL. */
    private fun endpointFor(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        // Most compatible gateways end at /v1, but some use /v4 (智谱),
        // /v1beta/openai (Gemini) or /openai (GitHub Models).
        val compatibleRoots = listOf("/v1", "/v2", "/v3", "/v4", "/openai", "/v1beta/openai")
        return when {
            base.endsWith("/chat/completions") -> base
            compatibleRoots.any { base.endsWith(it) } -> "$base/chat/completions"
            else -> "$base/v1/chat/completions"
        }
    }

    private fun httpErrorMessage(code: Int, raw: String): String {
        val detail = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()

        val friendly = when (code) {
            400 -> "请求被拒绝，可能是模型名不支持图片，请在设置里检查模型"
            401 -> "API Key 无效或已失效，请到设置里检查"
            402 -> "DeepSeek 账户余额不足"
            403 -> "该 API Key 没有访问权限"
            404 -> "接口地址不正确，请检查 Base URL"
            429 -> "请求太频繁，请稍后再试"
            in 500..599 -> "DeepSeek 服务暂时不可用（HTTP $code）"
            else -> "翻译失败（HTTP $code）"
        }
        return if (detail.isBlank()) friendly else "$friendly：$detail"
    }
}
