package com.qingyi.translate.data

/**
 * Languages offered in the picker. DeepSeek can handle far more than this list,
 * but a curated list keeps the picker quick to scan.
 *
 * [displayName] is what the UI shows; [promptName] is what we put into the
 * translation prompt (Chinese names work better with DeepSeek than ISO codes).
 */
enum class Language(
    val displayName: String,
    val promptName: String,
) {
    CHINESE_SIMPLIFIED("中文（简体）", "简体中文"),
    CHINESE_TRADITIONAL("中文（繁体）", "繁体中文"),
    ENGLISH("英语", "英语"),
    JAPANESE("日语", "日语"),
    KOREAN("韩语", "韩语"),
    FRENCH("法语", "法语"),
    GERMAN("德语", "德语"),
    SPANISH("西班牙语", "西班牙语"),
    PORTUGUESE("葡萄牙语", "葡萄牙语"),
    RUSSIAN("俄语", "俄语"),
    ITALIAN("意大利语", "意大利语"),
    ARABIC("阿拉伯语", "阿拉伯语"),
    THAI("泰语", "泰语"),
    VIETNAMESE("越南语", "越南语"),
    INDONESIAN("印尼语", "印尼语"),
    MALAY("马来语", "马来语"),
    HINDI("印地语", "印地语"),
    TURKISH("土耳其语", "土耳其语"),
    DUTCH("荷兰语", "荷兰语"),
    POLISH("波兰语", "波兰语"),
}
