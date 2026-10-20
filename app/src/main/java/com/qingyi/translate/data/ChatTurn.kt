package com.qingyi.translate.data

/** One message in the follow-up conversation, already mapped to API role names. */
data class ChatTurn(val role: String, val content: String) {
    companion object {
        const val USER = "user"
        const val ASSISTANT = "assistant"
    }
}
