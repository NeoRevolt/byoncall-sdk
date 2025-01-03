package com.dartmedia.byoncallsdk.model

data class MyCandidateModel(
    val type: String? = null,
    val candidate: MyNewCandidateModel? = null
)


data class MyNewCandidateModel(
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null
)
