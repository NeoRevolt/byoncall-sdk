package com.dartmedia.brandedlibrary.model

data class ChatModel(
    var id: String,
    val senderId: String,
    val conversationId: String,
    val message: String,
    val attachment: String,
    val createAt: Long,
    val receiverId: String,
    var isSelf: Boolean = false
)