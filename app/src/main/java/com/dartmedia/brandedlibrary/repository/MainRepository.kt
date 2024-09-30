package com.dartmedia.brandedlibrary.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.dartmedia.brandedlibrary.model.MyNewCandidateModel
import com.dartmedia.brandedlibrary.model.SocketDataModel
import com.dartmedia.brandedlibrary.model.SocketDataTypeEnum.Answer
import com.dartmedia.brandedlibrary.model.SocketDataTypeEnum.EndCall
import com.dartmedia.brandedlibrary.model.SocketDataTypeEnum.IceCandidates
import com.dartmedia.brandedlibrary.model.SocketDataTypeEnum.Offer
import com.dartmedia.brandedlibrary.model.UserStatusEnum
import com.dartmedia.brandedlibrary.socket.SocketClient
import com.dartmedia.brandedlibrary.webrtc.MyPeerObserver
import com.dartmedia.brandedlibrary.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val socketClient: SocketClient,
    private val webRTCClient: WebRTCClient,
    private val context: Context,
    private val gson: Gson
) : WebRTCClient.Listener {

    private var target: String? = null
    var listener: Listener? = null
    private var remoteView: SurfaceViewRenderer? = null

    fun observeTargetContact(target: String, status: (UserStatusEnum) -> Unit) {
        // TODO(Zal) : Make User Status logic when DB is available
        status(UserStatusEnum.IN_CALL) // Hardcoded
    }

    fun connectSocket(myUserId: String) {
        socketClient.connectSocket(myUserId)
    }

    fun disconnectSocket(function: () -> Unit) {
        socketClient.disconnectSocket(function)
    }

    fun observeSocket() {
        socketClient.observeSocketEvent(object : SocketClient.Listener {
            override fun onLatestEventReceived(event: SocketDataModel) {
                listener?.onLatestEventReceived(event)
                when (event.type) {
                    Offer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(target!!)
                        Log.d(TAG, "observeSocket: Offer from $target received, and sent Answer")
                    }

                    Answer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                        Log.d(TAG, "observeSocket: Answer from $target received")
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
                            Log.d(TAG, "observeSocket: ICE from $target received")
                        }
                    }

                    EndCall -> {
                        listener?.endCall()
                        Log.d(TAG, "observeSocket: EndCall from $target received")
                    }

                    else -> Unit
                }
                Log.d(TAG, "observeSocket : $event")
            }

        })
    }

    fun sendConnectionRequest(socketDataModel: SocketDataModel) {
        socketClient.sendMessageToSocket(socketDataModel)
    }

    fun recordCallLog() {
        //TODO(Zal) : Record Call Log when DB is available
    }

    fun setTarget(target: String) {
        this.target = target
    }

    interface Listener {
        fun onLatestEventReceived(data: SocketDataModel)
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

                    webRTCClient.sendIceCandidate(target!!, mIceCandidate)
                } catch (e: Exception) {
                    Log.e(TAG, "createPeerConnection Exception : $e")
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                when (newState) {
                    PeerConnection.PeerConnectionState.CONNECTING -> {
                        // 1. change my status to in calling
                        changeMyStatus(UserStatusEnum.CALLING)
                        // 2. clear latest event inside my user section in DB
//                        TODO(Zal) : Make User Status logic when DB is available
//                        firebaseClient.clearLatestEvent() //
                        Log.d(
                            TAG,
                            "onConnectionChange : connection with $target is CONNECTING"
                        )
                    }

                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        // 1. change my status to in call
                        changeMyStatus(UserStatusEnum.IN_CALL)
                        // 2. clear latest event inside my user section in DB
//                        TODO(Zal) : Make User Status logic when DB is available
//                        firebaseClient.clearLatestEvent() //
                        Log.d(
                            TAG,
                            "onConnectionChange : connection with $target is CONNECTED"
                        )
                    }

                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        Log.e(
                            TAG,
                            "onConnectionChange : connection with $target is DISCONNECTED"
                        )
                    }

                    PeerConnection.PeerConnectionState.FAILED -> {
                        Log.e(
                            TAG,
                            "onConnectionChange : connection with $target is FAILED"
                        )
                    }

                    PeerConnection.PeerConnectionState.CLOSED -> {
                        Log.e(
                            TAG,
                            "onConnectionChange : connection with $target is CLOSED"
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
            webRTCClient.call(target!!)
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
                    receiverId = target!!
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

    override fun onTransferEventToSocket(data: SocketDataModel) {
        socketClient.sendMessageToSocket(data)
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

    companion object {
        const val TAG = "MainRepository"
    }

}