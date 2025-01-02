package com.dartmedia.brandedsdk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dartmedia.brandedsdk.R
import com.dartmedia.brandedsdk.model.SocketDataModel
import com.dartmedia.brandedsdk.model.SocketDataTypeEnum
import com.dartmedia.brandedsdk.model.UserStatusEnum
import com.dartmedia.brandedsdk.model.isValid
import com.dartmedia.brandedsdk.repository.BrandedWebRTCClient
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.END_CALL
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.SETUP_VIEWS
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.START_SERVICE
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.STOP_SERVICE
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.SWITCH_CAMERA
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.TOGGLE_AUDIO
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.TOGGLE_AUDIO_DEVICE
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.TOGGLE_SCREEN_SHARE
import com.dartmedia.brandedsdk.service.BrandedServiceActionsEnum.TOGGLE_VIDEO
import com.dartmedia.brandedsdk.utils.audio.manager.RTCAudioManager
import org.webrtc.SurfaceViewRenderer


class BrandedService : Service(), BrandedWebRTCClient.Listener {

    private var isServiceRunning = false
    private var username: String? = null

    private val brandedWebRTCClient by lazy { BrandedWebRTCClient.instance(this) }

    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager
    private var isPreviousCallStateVideo = true


    companion object {
        const val TAG = "BrandedService"
        var callListener: CallListener? = null
        var inCallListener: InCallListener? = null
        var localSurfaceView: SurfaceViewRenderer? = null
        var remoteSurfaceView: SurfaceViewRenderer? = null
        var screenPermissionIntent: Intent? = null
    }

    override fun onCreate() {
        super.onCreate()
        rtcAudioManager = RTCAudioManager.create(this)
        rtcAudioManager.setDefaultAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE)
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { incomingIntent ->
            when (incomingIntent.action) {
                START_SERVICE.name -> handleStartService(incomingIntent)
                SETUP_VIEWS.name -> handleSetupViews(incomingIntent)
                END_CALL.name -> handleEndCall()
                SWITCH_CAMERA.name -> handleSwitchCamera()
                TOGGLE_AUDIO.name -> handleToggleAudio(incomingIntent)
                TOGGLE_VIDEO.name -> handleToggleVideo(incomingIntent)
                TOGGLE_AUDIO_DEVICE.name -> handleToggleAudioDevice(incomingIntent)
                TOGGLE_SCREEN_SHARE.name -> handleToggleScreenShare(incomingIntent)
                STOP_SERVICE.name -> handleStopService()
                else -> Unit
            }
        }

        return START_STICKY
    }

    private fun handleStopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        brandedWebRTCClient.endCall()
        brandedWebRTCClient.disconnectSocket {
            isServiceRunning = false
            stopSelf()
        }
    }

    private fun handleToggleScreenShare(incomingIntent: Intent) {
        startServiceWithNotification()
        val isStarting = incomingIntent.getBooleanExtra("isStarting", true)
        if (isStarting) {
            // Start Screen Share but first remove the camera streaming firs and mute the audio
            if (isPreviousCallStateVideo) {
                brandedWebRTCClient.toggleVideo(true)
            }
            brandedWebRTCClient.setScreenCaptureIntent(screenPermissionIntent!!)
            brandedWebRTCClient.toggleScreenShare(true)

        } else {
            // Stop Share Screen and check if camera streaming was on so make it on back again
            brandedWebRTCClient.toggleScreenShare(false)
            if (isPreviousCallStateVideo) {
                brandedWebRTCClient.toggleVideo(false)
            }
        }
    }

    private fun handleToggleAudioDevice(incomingIntent: Intent) {
        val type = when (incomingIntent.getStringExtra("type")) {
            RTCAudioManager.AudioDevice.EARPIECE.name -> RTCAudioManager.AudioDevice.EARPIECE
            RTCAudioManager.AudioDevice.SPEAKER_PHONE.name -> RTCAudioManager.AudioDevice.SPEAKER_PHONE
            else -> null
        }

        type?.let {
            rtcAudioManager.setDefaultAudioDevice(it)
            rtcAudioManager.selectAudioDevice(it)
            Log.d(TAG, "handleToggleAudioDevice: $it")
        }
    }

    private fun handleToggleVideo(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
        this.isPreviousCallStateVideo = !shouldBeMuted
        brandedWebRTCClient.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
        brandedWebRTCClient.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        brandedWebRTCClient.switchCamera()
    }

    private fun handleEndCall() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        //1. we have to send a signal to other peer that call is ended
        brandedWebRTCClient.sendEndCall()
        //2.end out call process and restart our webrtc client
        endCallAndRestartRepository()
    }

    private fun endCallAndRestartRepository() {
        brandedWebRTCClient.endCall()
        inCallListener?.onCallEnded()
        username?.let { username ->
            brandedWebRTCClient.initWebrtcClient(username)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        startServiceWithNotification()
        val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", true)
        val target = incomingIntent.getStringExtra("target")
        this.isPreviousCallStateVideo = isVideoCall
        brandedWebRTCClient.setTarget(target!!)
        brandedWebRTCClient.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        brandedWebRTCClient.initRemoteSurfaceView(remoteSurfaceView!!)

        if (!isCaller) {
            // start the video call
            brandedWebRTCClient.startCall()
        }

    }

    private fun handleStartService(incomingIntent: Intent) {
        if (!isServiceRunning) {
            isServiceRunning = true
            username = incomingIntent.getStringExtra("username")

            if (username == null) {
                Log.e(TAG, "handleStartService: username is null")
                return
            }

            // setup local client
            brandedWebRTCClient.listener = this
            brandedWebRTCClient.observeSocket()
            brandedWebRTCClient.initWebrtcClient(username!!)

        } else {
            Log.d(TAG, "handleStartService : Service is already running")
        }
    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "channel1", "foreground", NotificationManager.IMPORTANCE_DEFAULT
            )

            val intent = Intent(this, BrandedServiceReceiver::class.java).apply {
                action = "ACTION_END_CALL"
            }
            val pendingIntent: PendingIntent =
                PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            notificationManager.createNotificationChannel(notificationChannel)
            val notification = NotificationCompat.Builder(
                this, "channel1"
            ).setSmallIcon(R.drawable.ic_call_end_24)
                .addAction(R.drawable.ic_call_end_24, "End Call", pendingIntent)

            startForeground(1, notification.build())
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onLatestEventReceived(data: SocketDataModel) {
        if (data.isValid()) {
            when (data.type) {
                SocketDataTypeEnum.StartVideoCall,
                SocketDataTypeEnum.StartAudioCall -> {
                    callListener?.onCallReceived(data)
                }

                SocketDataTypeEnum.EndCall -> {
                    callListener?.onCallDeclined(data)
                }

                else -> Unit
            }
        } else {
            Log.d(TAG, "onLatestEventReceived: Data Invalid $data")
        }
    }

    override fun onCallStatusChanged(userStatusEnum: UserStatusEnum) {
        inCallListener?.onCallStatusChanged(userStatusEnum)
    }

    override fun endCall() {
        // when retrieving end call from other peer
        stopForeground(STOP_FOREGROUND_REMOVE)
        endCallAndRestartRepository()
    }

    interface CallListener {
        fun onCallReceived(model: SocketDataModel)
        fun onCallDeclined(model: SocketDataModel)
    }

    interface InCallListener {
        fun onCallStatusChanged(userStatusEnum: UserStatusEnum)
        fun onCallEnded()
    }
}