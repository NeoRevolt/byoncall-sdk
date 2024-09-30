package com.dartmedia.brandedlibrary.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.dartmedia.brandedlibrary.model.MyNewCandidateModel
import com.dartmedia.brandedlibrary.model.SocketDataModel
import com.dartmedia.brandedlibrary.model.SocketDataTypeEnum
import com.google.gson.Gson
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    // Class variables
    var listener: Listener? = null
    private lateinit var username: String

    // Webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val rtcConfig by lazy { initRTCConfig() }
    private var peerConnection: PeerConnection? = null
    private val iceServer = listOf(
        // Pak Iman Custom Server
        // TODO(Zal): Testing STUN/TURN Server with Pak Iman
        PeerConnection.IceServer.builder("turn:103.39.68.184:3478")
            .setUsername("kavirajan")
            .setPassword("123456").createIceServer()

        // Community Free TURN Server alternative
        //PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
        //    .setUsername("83eebabf8b4cce9d5dbcb649")
        //    .setPassword("2D7JvfkOQtBdYW3R").createIceServer()
    )
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    // Call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream? = null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    // Share Screen
    private var permissionIntent: Intent? = null
    private var screenCapturer: VideoCapturer? = null
    private val localScreenVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private var localScreenShareVideoTrack: VideoTrack? = null

    // Init requirement
    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBaseContext)
            ).setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBaseContext, true, true
                )
            ).setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = false
                disableEncryption = false
            }).createPeerConnectionFactory()
    }

    /** Init User WebRTC Client */
    fun initializeWebrtcClient(
        username: String, observer: PeerConnection.Observer
    ) {
        //TODO(Zal): username or IDs must not have whitespace !
        this.username = username
        localTrackId = "${username}_track"
        localStreamId = "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(rtcConfig, observer)
    }

    // Call Offer
    fun call(target: String) {
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        Log.d(TAG, "onSetSuccess : $desc.description")
                        listener?.onTransferEventToSocket(
                            SocketDataModel(
                                type = SocketDataTypeEnum.Offer,
                                senderId = username,
                                receiverId = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }

            override fun onCreateFailure(p0: String?) {
                super.onCreateFailure(p0)
                Log.d(TAG, "onCreateFailure : $p0")
            }
        }, mediaConstraint)
    }

    // Call Answer
    fun answer(target: String) {
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            SocketDataModel(
                                type = SocketDataTypeEnum.Answer,
                                senderId = username,
                                receiverId = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(target: String, iceCandidate: MyNewCandidateModel) {
        listener?.onTransferEventToSocket(
            SocketDataModel(
                type = SocketDataTypeEnum.IceCandidates,
                senderId = username,
                receiverId = target,
                data = iceCandidate
            )
        )
    }

    private fun initRTCConfig(): PeerConnection.RTCConfiguration {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServer).apply {

            //sdpSemantics: Set to UNIFIED_PLAN for better compatibility and future-proofing.
            // TODO : UNIFIED_PLAN tidak bisa menggunakan localStream (MediaStream), sehingga harus diubah menjadi hanya track (AudioTrack)
//            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

            // iceTransportPolicy: Set to ALL to use all available transport protocols.
            iceTransportsType = PeerConnection.IceTransportsType.RELAY

            // bundlePolicy: Set to MAXBUNDLE to reduce the number of ICE connections and optimize bandwidth.
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE

            // rtcpMuxPolicy: Set to REQUIRE to use a single port for RTP and RTCP, reducing overhead.
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE

            // audioJitterBufferMaxPackets: Increased to 50 to allow for more buffering of audio packets, which helps in unstable network conditions.
            audioJitterBufferMaxPackets = 50

            // audioJitterBufferFastAccelerate: Enabled to speed up the handling of audio jitter.
            audioJitterBufferFastAccelerate = true

            // *tcpCandidatePolicy: Set to ENABLED to allow TCP candidates, which can help in restrictive network environments.
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED

            // continualGatheringPolicy: Set to GATHER_CONTINUALLY to keep ICE gathering on for better connectivity.
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

            // iceConnectionReceivingTimeout: Set to 3000 ms for quicker detection of connection issues.
            iceConnectionReceivingTimeout = 3000

            // keyType: Set to ECDSA for more efficient key handling.
            keyType = PeerConnection.KeyType.ECDSA

            // enableCpuOveruseDetection: Enabled to dynamically adjust quality based on CPU usage.
            enableCpuOveruseDetection = true
        }
        return rtcConfig
    }

    fun closeConnection() {
        try {
            videoCapturer.dispose()
            screenCapturer?.dispose()
            localStream?.dispose()
            peerConnection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera() {
        videoCapturer.switchCamera(null)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        if (shouldBeMuted) {
            localStream?.removeTrack(localAudioTrack)
        } else {
            localStream?.addTrack(localAudioTrack)
        }
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        try {
            if (shouldBeMuted) {
                stopCapturingCamera()
            } else {
                startCapturingCamera(localSurfaceView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Streaming
    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        this.remoteSurfaceView = view
        initSurfaceView(view)
    }

    fun initLocalSurfaceView(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        this.localSurfaceView = localView
        initSurfaceView(localView)
        startLocalStreaming(localView, isVideoCall)
    }

    private fun startLocalStreaming(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if (isVideoCall) {
            startCapturingCamera(localView)
        }

        localAudioTrack =
            peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    private fun startCapturingCamera(localView: SurfaceViewRenderer) {
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )

        videoCapturer.initialize(
            surfaceTextureHelper, context, localVideoSource.capturerObserver
        )

        videoCapturer.startCapture(
            720, 480, 20
        )

        localVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }

    private fun getVideoCapturer(context: Context): CameraVideoCapturer =
        Camera2Enumerator(context).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }

    private fun stopCapturingCamera() {

        videoCapturer.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }

    // Screen Share
    fun setPermissionIntent(screenPermissionIntent: Intent) {
        this.permissionIntent = screenPermissionIntent
    }

    fun startScreenCapturing() {
        val displayMetrics = DisplayMetrics()
        val windowsManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowsManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels

        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )

        screenCapturer = createScreenCapturer()
        screenCapturer!!.initialize(
            surfaceTextureHelper, context, localScreenVideoSource.capturerObserver
        )
        screenCapturer!!.startCapture(screenWidthPixels, screenHeightPixels, 15)

        localScreenShareVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId + "_video", localScreenVideoSource)
        localScreenShareVideoTrack?.addSink(localSurfaceView)
        localStream?.addTrack(localScreenShareVideoTrack)
        peerConnection?.addStream(localStream)

    }

    fun stopScreenCapturing() {
        screenCapturer?.stopCapture()
        screenCapturer?.dispose()
        localScreenShareVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localScreenShareVideoTrack)
        localScreenShareVideoTrack?.dispose()

    }

    private fun createScreenCapturer(): VideoCapturer {
        return ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d(TAG, "onStop: permission of screen casting is stopped")
            }
        })
    }

    companion object {
        const val TAG = "WebRTCClient"
    }


    interface Listener {
        fun onTransferEventToSocket(data: SocketDataModel)
    }
}