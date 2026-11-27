package com.qingyi.translate.data

/**
 * The translation prompts ask the model to start its answer with a marker such
 * as `[[英语]]`. Splitting it off lets the UI show which language was detected
 * while the translation itself stays clean.
 */
object LanguageTag {

    private val complete = Regex("^\\s*\\[\\[([^\\]]+)]]\\s*\\n?")
    private val incomplete = Regex("^\\s*\\[\\[[^\\]]*$")

    /** Returns the detected language (or null) and the text after the marker. */
    fun split(raw: String): Pair<String?, String> {
        val match = complete.find(raw) ?: return null to raw
        return match.groupValues[1].trim() to raw.substring(match.range.last + 1)
    }

    /** Hides a half-received marker so the UI never flashes `[[英`. */
    fun visible(raw: String): String {
        if (incomplete.containsMatchIn(raw)) return ""
        return split(raw).second
    }
}
