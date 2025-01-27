package com.dartmedia.byoncallsdk.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.dartmedia.byoncallsdk.model.MyNewCandidateModel
import com.dartmedia.byoncallsdk.model.SocketDataModel
import com.dartmedia.byoncallsdk.model.SocketDataTypeEnum.Answer
import com.dartmedia.byoncallsdk.model.SocketDataTypeEnum.EndCall
import com.dartmedia.byoncallsdk.model.SocketDataTypeEnum.IceCandidates
import com.dartmedia.byoncallsdk.model.SocketDataTypeEnum.Offer
import com.dartmedia.byoncallsdk.model.UserStatusEnum
import com.dartmedia.byoncallsdk.socket.SocketClientByonCall
import com.dartmedia.byoncallsdk.webrtc.MyPeerObserver
import com.dartmedia.byoncallsdk.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer

class ByonCallRepository(
    private val context: Context
) : WebRTCClient.Listener {

    companion object {
        fun instance(context: Context): ByonCallRepository {
            return ByonCallRepository(context)
        }

        val TAG = ByonCallRepository::class.java.simpleName.toString()
    }

    private val webRTCClient by lazy { WebRTCClient.instance(context) }
    private val socketClientSdk by lazy { SocketClientByonCall }

    private val gson = Gson()

    private var targetPhone: String? = null
    var listener: Listener? = null
    private var remoteView: SurfaceViewRenderer? = null


    fun observeTargetContact(target: String, status: (UserStatusEnum) -> Unit) {
        // TODO: change to webRTC connection status
        status(UserStatusEnum.IN_CALL)
    }

    fun connectSocket(socketUrl: String, myPhone: String) {
        socketClientSdk.connectSocket(myPhone = myPhone, socketUrl = socketUrl)
    }

    fun disconnectSocket(function: () -> Unit) {
        socketClientSdk.disconnectSocket()
    }

    fun observeSocket() {
        socketClientSdk.observeSocketEvent(object : SocketClientByonCall.SocketListener {
            override fun onLatestSocketEvent(event: SocketDataModel) {
                listener?.onLatestEventReceived(event)
                when (event.type) {
                    Offer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(targetPhone!!)
                        Log.d(
                            TAG,
                            "observeSocket: Offer from $targetPhone received, and sent Answer"
                        )
                    }

                    Answer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                        Log.d(TAG, "observeSocket: Answer from $targetPhone received")
                    }

                    IceCandidates -> {
                        val mIceCandidate: MyNewCandidateModel? = try {
                            gson.fromJson(
                                gson.toJson(event.data),
                                MyNewCandidateModel::class.java
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to convert candidate")
                            null
                        }

                        mIceCandidate?.let { candidate ->
                            val iceCandidateModel = IceCandidate(
                                candidate.sdpMid,
                                candidate.sdpMLineIndex!!,
                                candidate.candidate
                            )
                            webRTCClient.addIceCandidateToPeer(iceCandidateModel)
                            Log.d(TAG, "observeSocket: ICE from $targetPhone received")
                        }
                    }

                    EndCall -> {
                        listener?.endCall()
                        Log.d(TAG, "observeSocket: EndCall from $targetPhone received")
                    }

                    else -> Unit
                }
                Log.d(TAG, "observeSocket : $event")
            }

        })
    }

    fun sendConnectionRequest(socketDataModel: SocketDataModel) {
        socketClientSdk.sendEventToSocket(socketDataModel)
    }

    fun recordCallLog() {
        //TODO(Zal) : Record Call Log when DB is available
    }

    fun setTarget(target: String) {
        this.targetPhone = target
    }

    interface Listener {
        fun onLatestEventReceived(data: SocketDataModel)
        fun onCallStatusChanged(userStatusEnum: UserStatusEnum)
        fun endCall()
    }

    fun initWebrtcClient(username: String) {
        webRTCClient.listener = this
        webRTCClient.initializeWebrtcClient(username, object : MyPeerObserver() {

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                try {
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)

                try {
                    val mIceCandidate = MyNewCandidateModel(
                        candidate = p0?.sdp,
                        sdpMLineIndex = p0?.sdpMLineIndex,
                        sdpMid = p0?.sdpMid
                    )

                    webRTCClient.sendIceCandidate(targetPhone!!, mIceCandidate)
                } catch (e: Exception) {
                    Log.e(TAG, "createPeerConnection Exception : $e")
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTING -> {
                        listener?.onCallStatusChanged(UserStatusEnum.CALLING)
                        Log.d(
                            TAG,
                            "onConnectionChange : connection with $targetPhone is CONNECTING"
                        )
                    }

                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        listener?.onCallStatusChanged(UserStatusEnum.IN_CALL)
                        Log.d(
                            TAG,
                            "onConnectionChange : connection with $targetPhone is CONNECTED"
                        )
                    }

                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        listener?.onCallStatusChanged(UserStatusEnum.CALLING)
                        Log.e(
                            TAG,
                            "onConnectionChange : connection with $targetPhone is DISCONNECTED"
                        )
                    }

                    PeerConnection.PeerConnectionState.FAILED -> {
                        listener?.onCallStatusChanged(UserStatusEnum.FAILED)
                        Log.e(
                            TAG,
                            "onConnectionChange : connection with $targetPhone is FAILED"
                        )
                    }

                    PeerConnection.PeerConnectionState.CLOSED -> {
                        listener?.onCallStatusChanged(UserStatusEnum.OFFLINE)
                        Log.e(
                            TAG,
                            "onConnectionChange : connection with $targetPhone is CLOSED"
                        )
                    }

                    else -> {}
                }
            }
        })
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall: Boolean) {
        webRTCClient.initLocalSurfaceView(view, isVideoCall)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
        this.remoteView = view
    }


    fun startCall() {
        try {
            webRTCClient.call(targetPhone!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "StartCall() Exception: ${e.cause}")
        }
    }

    fun endCall() {
        webRTCClient.closeConnection()
        changeMyStatus(UserStatusEnum.ONLINE)
    }

    fun sendEndCall() {
        try {
            onTransferEventToSocket(
                SocketDataModel(
                    type = EndCall,
                    receiverId = targetPhone!!
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "senEndCall() Exception: ${e.cause}")
        }

    }

    fun sendRejectCall(socketDataModel: SocketDataModel) {
        onTransferEventToSocket(
            socketDataModel.copy(
                senderId = socketDataModel.receiverId,
                type = EndCall,
                receiverId = socketDataModel.senderId
            )
        )
    }

    private fun changeMyStatus(status: UserStatusEnum) {
        //TODO(Zal) : Change user status in DB, when DB is available
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    fun setScreenCaptureIntent(screenPermissionIntent: Intent) {
        webRTCClient.setPermissionIntent(screenPermissionIntent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        if (isStarting) {
            webRTCClient.startScreenCapturing()
        } else {
            webRTCClient.stopScreenCapturing()
        }
    }

    override fun onTransferEventToSocket(data: SocketDataModel) {
        socketClientSdk.sendEventToSocket(data)
    }


}