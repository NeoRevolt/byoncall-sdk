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

/**
 * This class handles :
 * - Sessions
 * - Calls
 * - Listeners
 *
 * @property context
 */
class ByonCallSDK private constructor(
    private val context: Context,
) : ServiceCallByon.CallListener, ServiceCallByon.InCallListener {

    private val socketClient by lazy { SocketClientByonCall }
    private val webRTCClient by lazy { ByonCallRepository.instance(context) }
    private val serviceClient by lazy { ServiceClientByonCall.instance(context) }

    /**
     * Listener for receive Incoming Call and also Declined Call
     *
     * - [onCallReceived] Incoming call request from other Peers
     *
     * - [onCallDeclined] Declined happened when the other Peers suddenly close the call before you even Answer
     */
    interface CallListener {
        fun onCallReceived(model: SocketDataModel)
        fun onCallDeclined(model: SocketDataModel)
    }

    /**
     * In-Call Listener to receive the Connection Status and End Call when Calling
     *
     * - [onCallReceived] Incoming call request from other Peers
     *
     * - [onCallDeclined] Declined happened when the other Peers suddenly close the call before you even Answer
     *
     */
    interface InCallListener {
        fun onCallStatusChanged(statusEnum: UserStatusEnum)
        fun onCallEnded()
    }

    /**
     * Listener for receive Incoming Call and also Declined Call
     *
     * - [onCallReceived] Incoming call request from other Peers
     *
     * - [onCallDeclined] Declined happened when the other Peers suddenly close the call before you even Answer
     */
    var callListener: CallListener? = null

    /**
     * In-Call Listener to receive the Connection Status and End Call when Calling
     *
     * - [onCallReceived] Incoming call request from other Peers
     *
     * - [onCallDeclined] Declined happened when the other Peers suddenly close the call before you even Answer
     *
     */
    var inCallListener: InCallListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: ByonCallSDK? = null

        /**
         * Inititate Session, also will create an [instance]
         * , [connectSocket], and [startService]
         *
         *
         *
         * #### Example usage :
         * ```
         * private var byonCallSDK: ByonCallSDK? = null
         *
         * byonCallSDK = ByonCallSDK.startSession(
         *                 context,
         *                 socketUrl = "http://localhost:8000/socket/private",
         *                 myPhone = "+62819xxxxxxx"
         * )
         * ```
         *
         * Also when you have [startSession], you can also use [getInstance] so you don't need to
         * start session over and over again.
         *
         * @param context Application owner for running Call Service such as Video Call with ShareScreen
         * @param socketUrl Socket URL used for Signaling server
         * @param myPhone Phone number used as ID
         *
         * @return [instance] of [ByonCallSDK] when successfully started the session
         */
        fun startSession(
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

        /**
         * getInstance of ByonCallSDK,
         *
         * - note : when you already [startSession], you can use this function to get the ByonCallSDK instance.
         * So make sure you have start the session properly.
         *
         * #### Example usage :
         * ```
         * private var byonCallSDK = ByonCallSDK.getInstance()
         * ```
         *
         * @return [instance]
         * @throws [IllegalStateException] "CallSDK is not initialized. startSession() first."
         */
        fun getInstance(): ByonCallSDK {
            return instance
                ?: throw IllegalStateException("CallSDK is not initialized. startSession() first.")
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


    /**
     * Validate instance / session initialization.
     */
    private fun validateSession() {
        if (instance == null) {
            throw IllegalStateException("CallSDK is not initialized. startSession() first.")
        }
    }

    /**
     * Stop the session and disconnect from socket
     *
     * - note: [stopSession] doesn't send EndCall to other Peers, it just close the session and the services
     * for End Call you can use [endCall] instead.
     */
    fun stopSession() {
        serviceClient.stopService()
    }

    /**
     * Start call
     *
     * @param socketDataModel
     */
    fun startCall(socketDataModel: SocketDataModel) {
        validateSession()
        webRTCClient.sendConnectionRequest(socketDataModel)
    }

    /**
     * Reject call
     *
     * @param socketDataModel
     */
    fun rejectCall(socketDataModel: SocketDataModel) {
        validateSession()
        webRTCClient.sendRejectCall(socketDataModel)
    }

    /**
     * Send EndCall to other Peers also clear all WebRTC Connection and restore all call services
     *
     * - note [endCall] will keep you connected to the socket with the same ID/Phone number
     *
     */
    fun endCall() {
        validateSession()
        serviceClient.sendEndCall()
    }

    /**
     * Disconnect socket
     *
     */
    fun disconnectSocket() {
        validateSession()
        socketClient.disconnectSocket()
    }

    /**
     * Record call log
     *
     */
    fun recordCallLog() {
        validateSession()
        webRTCClient.recordCallLog()
    }

    /**
     * Observe target contact (Optional experimental)
     *
     * @param target
     * @param status
     * @receiver
     */
    fun observeTargetContact(target: String, status: (UserStatusEnum) -> Unit) {
        validateSession()
        webRTCClient.observeTargetContact(target) {
            status(it)
        }
    }

    /**
     * Send chat to socket
     *
     * @param socketDataModel
     */
    fun sendChatToSocket(socketDataModel: SocketDataModel) {
        validateSession()
        socketClient.sendEventToSocket(socketDataModel)
    }

    /**
     * Observe chat from socket
     *
     * @param owner
     * @param chat
     * @receiver
     */
    fun observeChatFromSocket(
        owner: AppCompatActivity,
        chat: (SocketDataModel) -> Unit
    ) {
        validateSession()
        socketClient.onLatestChat.observe(owner) {
            chat(it)
        }
    }

    /**
     * Toggle screen share
     *
     * @param isStarting
     */
    fun toggleScreenShare(isStarting: Boolean) {
        validateSession()
        serviceClient.toggleScreenShare(isStarting)
    }

    /**
     * Toggle audio
     *
     * @param shouldBeMuted
     */
    fun toggleAudio(shouldBeMuted: Boolean) {
        validateSession()
        serviceClient.toggleAudio(shouldBeMuted)
    }

    /**
     * Toggle audio device
     *
     * @param type
     */
    fun toggleAudioDevice(type: String) {
        validateSession()
        serviceClient.toggleAudioDevice(type)
    }

    /**
     * Switch camera
     *
     */
    fun switchCamera() {
        validateSession()
        serviceClient.switchCamera()
    }

    /**
     * Toggle video
     *
     * @param shouldBeMuted
     */
    fun toggleVideo(shouldBeMuted: Boolean) {
        validateSession()
        serviceClient.toggleVideo(shouldBeMuted)
    }


    /**
     * Setup Call Views
     *
     * - note : This function only be used in Activity where there's [SurfaceViewRenderer] in your
     * layout view for example CallActivity
     *
     * when you start a call or accepting incoming call from the [onCallReceived] and move to you CallActivity
     * then you can use this function to setup and start the process call.
     *
     * #### Important :
     *  - if you are the caller a.k.a the one who clicks the call-button then the [isCaller] is true
     *  - if you are the receiver a.k.a the one who accept the incoming call from [onCallReceived] then the [isCaller] is false
     *
     * #### Example usage :
     *
     * ```
     * byonCallSDK.setupViews(
     *                 isVideoCall = true,
     *                 isCaller = true,
     *                 target = "+62819xxxxxxxx",
     *                 localSurfaceView = localView,
     *                 remoteSurfaceView = remoteView
     * )
     * ```
     *
     * @param isVideoCall is the call video call or not
     * @param isCaller see the "Important" message above
     * @param target other Peers ID/Phone Number
     * @param localSurfaceView your [SurfaceViewRenderer]
     * @param remoteSurfaceView other peers [SurfaceViewRenderer]
     */
    fun setupViews(
        isVideoCall: Boolean,
        isCaller: Boolean,
        target: String,
        localSurfaceView: SurfaceViewRenderer?,
        remoteSurfaceView: SurfaceViewRenderer?
    ) {
        validateSession()
        serviceClient.setupViews(isVideoCall, isCaller, target)
        setSurfaceView(localSurfaceView, remoteSurfaceView)
    }

    /**
     * Set surface view
     *
     * @param local
     * @param remote
     */
    private fun setSurfaceView(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        validateSession()
        ServiceCallByon.apply {
            localSurfaceView = local
            remoteSurfaceView = remote
        }
    }

    /**
     * Clear surface view when the Activity is destroyed
     *
     */
    fun clearSurfaceView() {
        validateSession()
        try {
            ServiceCallByon.apply {
                remoteSurfaceView?.release()
                remoteSurfaceView = null
                localSurfaceView?.release()
                localSurfaceView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Set screen permission intent
     *
     * @param intent
     */
    fun setScreenPermissionIntent(intent: Intent?) {
        validateSession()
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