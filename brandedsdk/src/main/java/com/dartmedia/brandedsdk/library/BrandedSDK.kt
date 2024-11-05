package com.dartmedia.brandedsdk.library

import android.annotation.SuppressLint
import android.content.Context
import com.dartmedia.brandedsdk.model.SocketDataModel
import com.dartmedia.brandedsdk.model.UserStatusEnum
import com.dartmedia.brandedsdk.repository.BrandedWebRTCClient
import com.dartmedia.brandedsdk.service.BrandedService
import com.dartmedia.brandedsdk.service.BrandedServiceClient
import com.dartmedia.brandedsdk.socket.BrandedSocketClient

class BrandedSDK private constructor(
    private val context: Context,
) : BrandedService.Listener, BrandedService.EndCallListener {

    private val socketClient by lazy { BrandedSocketClient }
    private val webRTCClient by lazy { BrandedWebRTCClient.instance(context) }
    private val serviceClient by lazy { BrandedServiceClient.instance(context) }

    interface CallListener {
        fun onCallReceived(model: SocketDataModel)
        fun onCallDeclined(model: SocketDataModel)
    }

    interface EndCallListener {
        fun onCallEnded()
    }

    var callListener: CallListener? = null
    var endCallListener: EndCallListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: BrandedSDK? = null

        fun initialize(
            context: Context,
        ): BrandedSDK {
            return instance ?: synchronized(this) {
                instance ?: BrandedSDK(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(): BrandedSDK {
            return instance
                ?: throw IllegalStateException("LibraryManager is not initialized. Call initialize() first.")
        }
    }

    init {
        BrandedService.listener = this
        BrandedService.endCallListener = this
    }

    fun connectSocket(socketUrl: String, phoneNumber: String) {
        webRTCClient.connectSocket(socketUrl = socketUrl, myPhone = phoneNumber)
    }

    fun sendRejectCall(socketDataModel: SocketDataModel) {
        webRTCClient.sendRejectCall(socketDataModel)
    }

    fun recordCallLog() {
        webRTCClient.recordCallLog()
    }

    fun observeTargetContact(target: String, status: (UserStatusEnum) -> Unit) {
        webRTCClient.observeTargetContact(target) {
            status(it)
        }
    }

    fun disconnectSocket() {
        socketClient.disconnectSocket()
    }

    fun startService(phoneNumber: String) {
        serviceClient.startService(username = phoneNumber)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        serviceClient.toggleScreenShare(isStarting)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        serviceClient.toggleAudio(shouldBeMuted)
    }

    fun toggleAudioDevice(type: String) {
        serviceClient.toggleAudioDevice(type)
    }

    fun switchCamera() {
        serviceClient.switchCamera()
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        serviceClient.toggleVideo(shouldBeMuted)
    }

    fun sendEndCall() {
        serviceClient.sendEndCall()
    }

    fun setupViews(isVideoCall: Boolean, isCaller: Boolean, target: String) {
        serviceClient.setupViews(isVideoCall, isCaller, target)
    }

    override fun onCallReceived(model: SocketDataModel) {
        callListener?.onCallReceived(model)
    }

    override fun onCallDeclined(model: SocketDataModel) {
        callListener?.onCallDeclined(model)
    }

    override fun onCallEnded() {
        endCallListener?.onCallEnded()
    }
}