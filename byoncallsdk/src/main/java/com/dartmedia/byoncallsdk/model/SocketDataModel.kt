package com.dartmedia.byoncallsdk.model

import kotlinx.serialization.Serializable


enum class SocketDataTypeEnum {
    StartChatting, StartAudioCall, StartVideoCall, Offer, Answer, IceCandidates, EndCall
}

@Serializable
data class SocketDataModel(
    val type: SocketDataTypeEnum? = null,
    val senderName: String? = null,
    val senderId: String? = null,
    val receiverId: String? = null,
    val data: Any? = null,
    val senderImage: String? = null,
    val callMessage: String? = null,
    val timeStamp: Long = System.currentTimeMillis()
)

fun SocketDataModel.isValid(): Boolean {
    return java.lang.System.currentTimeMillis() - this.timeStamp < 60000 // Not using the event if passed one minute
}