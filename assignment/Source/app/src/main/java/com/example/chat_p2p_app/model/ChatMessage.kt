package com.example.chat_p2p_app.model

import java.util.Date

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    val messageType: MessageType = MessageType.TEXT,
    val isFromMe: Boolean = false
) {
    constructor() : this("", "", "", "", Date(), MessageType.TEXT, false)
}

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}
