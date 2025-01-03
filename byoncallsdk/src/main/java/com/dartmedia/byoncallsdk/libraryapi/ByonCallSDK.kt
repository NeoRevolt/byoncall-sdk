package com.dartmedia.byoncallsdk.libraryapi

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.dartmedia.byoncallsdk.model.SocketDataModel
import com.dartmedia.byoncallsdk.model.UserStatusEnum
import com.dartmedia.byoncallsdk.repository.ByonCallRepository
import com.dartmedia.byoncallsdk.service.ServiceCallByon
import com.dartmedia.byoncallsdk.service.ServiceClientByonCall
import com.dartmedia.byoncallsdk.socket.SocketClientByonCall
import org.webrtc.SurfaceViewRenderer

class ByonCallSDK private constructor(
    private val context: Context,
) : ServiceCallByon.CallListener, ServiceCallByon.InCallListener {

    private val socketClient by lazy { SocketClientByonCall }
    private val webRTCClient by lazy { ByonCallRepository.instance(context) }
    private val serviceClient by lazy { ServiceClientByonCall.instance(context) }

    interface CallListener {
        fun onCallReceived(model: SocketDataModel)
        fun onCallDeclined(model: SocketDataModel)
    }

    interface InCallListener {
        fun onCallStatusChanged(statusEnum: UserStatusEnum)
        fun onCallEnded()
    }

    var callListener: CallListener? = null
    var inCallListener: InCallListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ByonCallSDK? = null

        fun initialize(
            context: Context,
            socketUrl: String,
            myPhone: String
        ): ByonCallSDK {
            return instance ?: synchronized(this) {
                instance ?: ByonCallSDK(context.applicationContext).also {
                    instance = it
                    it.connectSocket(socketUrl, myPhone)
                    it.startService(myPhone)
                }
            }
        }

        fun getInstance(): ByonCallSDK {
            return instance
                ?: throw IllegalStateException("BrandedSDK is not initialized. Call BrandedSDK.initialize() first.")
        }
    }

    init {
        ServiceCallByon.callListener = this
        ServiceCallByon.inCallListener = this
    }

    private fun connectSocket(socketUrl: String, phoneNumber: String) {
        webRTCClient.connectSocket(socketUrl = socketUrl, myPhone = phoneNumber)
    }

    private fun startService(phoneNumber: String) {
        serviceClient.startService(username = phoneNumber)
    }


    fun startCall(socketDataModel: SocketDataModel) {
        webRTCClient.sendConnectionRequest(socketDataModel)
    }

    fun rejectCall(socketDataModel: SocketDataModel) {
        webRTCClient.sendRejectCall(socketDataModel)
    }

    fun endCall() {
        serviceClient.sendEndCall()
    }

    fun disconnectSocket() {
        socketClient.disconnectSocket()
    }

    fun stopService() {
        serviceClient.stopService()
    }

    fun recordCallLog() {
        webRTCClient.recordCallLog()
    }

    fun observeTargetContact(target: String, status: (UserStatusEnum) -> Unit) {
        webRTCClient.observeTargetContact(target) {
            status(it)
        }
    }

    fun sendChatToSocket(socketDataModel: SocketDataModel) {
        socketClient.sendEventToSocket(socketDataModel)
    }

    fun observeChatFromSocket(
        owner: AppCompatActivity,
        chat: (SocketDataModel) -> Unit
    ) {
        socketClient.onLatestChat.observe(owner) {
            chat(it)
        }
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


    fun setupViews(
        isVideoCall: Boolean,
        isCaller: Boolean,
        target: String,
        localSurfaceView: SurfaceViewRenderer?,
        remoteSurfaceView: SurfaceViewRenderer?
    ) {
        serviceClient.setupViews(isVideoCall, isCaller, target)
        setSurfaceView(localSurfaceView, remoteSurfaceView)
    }

    fun setSurfaceView(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        ServiceCallByon.apply {
            localSurfaceView = local
            remoteSurfaceView = remote
        }
    }

    fun clearSurfaceView() {
        ServiceCallByon.apply {
            remoteSurfaceView?.release()
            remoteSurfaceView = null
            localSurfaceView?.release()
            localSurfaceView = null
        }
    }

    fun setScreenPermissionIntent(intent: Intent?) {
        ServiceCallByon.screenPermissionIntent = intent
    }

    override fun onCallReceived(model: SocketDataModel) {
        callListener?.onCallReceived(model)
    }

    override fun onCallDeclined(model: SocketDataModel) {
        callListener?.onCallDeclined(model)
    }

    override fun onCallStatusChanged(userStatusEnum: UserStatusEnum) {
        inCallListener?.onCallStatusChanged(userStatusEnum)
    }

    override fun onCallEnded() {
        inCallListener?.onCallEnded()
    }
}