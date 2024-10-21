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
import com.dartmedia.brandedsdk.model.isValid
import com.dartmedia.brandedsdk.repository.WebRTCRepository
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.END_CALL
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.SETUP_VIEWS
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.START_SERVICE
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.STOP_SERVICE
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.SWITCH_CAMERA
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.TOGGLE_AUDIO
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.TOGGLE_AUDIO_DEVICE
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.TOGGLE_SCREEN_SHARE
import com.dartmedia.brandedsdk.service.MainServiceActionsEnum.TOGGLE_VIDEO
import com.dartmedia.brandedsdk.utils.audio.manager.RTCAudioManager
import dagger.hilt.android.AndroidEntryPoint
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class MainService : Service(), WebRTCRepository.Listener {

    private var isServiceRunning = false
    private var username: String? = null

    @Inject
    lateinit var webRTCRepository: WebRTCRepository

    private lateinit var notificationManager: NotificationManager
    private lateinit var rtcAudioManager: RTCAudioManager
    private var isPreviousCallStateVideo = true


    companion object {
        const val TAG = "MainService"
        var listener: Listener? = null
        var endCallListener: EndCallListener? = null
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
        webRTCRepository.endCall()
        webRTCRepository.disconnectSocket {
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
                webRTCRepository.toggleVideo(true)
            }
            webRTCRepository.setScreenCaptureIntent(screenPermissionIntent!!)
            webRTCRepository.toggleScreenShare(true)

        } else {
            // Stop Share Screen and check if camera streaming was on so make it on back again
            webRTCRepository.toggleScreenShare(false)
            if (isPreviousCallStateVideo) {
                webRTCRepository.toggleVideo(false)
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
        webRTCRepository.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
        webRTCRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        webRTCRepository.switchCamera()
    }

    private fun handleEndCall() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        //1. we have to send a signal to other peer that call is ended
        webRTCRepository.sendEndCall()
        //2.end out call process and restart our webrtc client
        endCallAndRestartRepository()
    }

    private fun endCallAndRestartRepository() {
        webRTCRepository.endCall()
        endCallListener?.onCallEnded()
        username?.let { username ->
            webRTCRepository.initWebrtcClient(username)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        startServiceWithNotification()
        val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", true)
        val target = incomingIntent.getStringExtra("target")
        this.isPreviousCallStateVideo = isVideoCall
        webRTCRepository.setTarget(target!!)
        webRTCRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        webRTCRepository.initRemoteSurfaceView(remoteSurfaceView!!)

        if (!isCaller) {
            // start the video call
            webRTCRepository.startCall()
        }

    }

    private fun handleStartService(incomingIntent: Intent) {
        //TODO (Zal) : Handle Service Foreground / Background
        if (!isServiceRunning) {
            isServiceRunning = true
            username = incomingIntent.getStringExtra("username")

            // setup local client
            webRTCRepository.listener = this
            webRTCRepository.observeSocket()
            webRTCRepository.initWebrtcClient(username!!)

        }
    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "channel1", "foreground", NotificationManager.IMPORTANCE_DEFAULT
            )

            val intent = Intent(this, MainServiceReceiver::class.java).apply {
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
                    listener?.onCallReceived(data)
                }

                SocketDataTypeEnum.EndCall -> {
                    listener?.onCallDeclined(data)
                }

                else -> Unit
            }
        } else {
            Log.d(TAG, "onLatestEventReceived: Data Invalid $data")
        }
    }

    override fun endCall() {
        // when retrieving end call from other peer
        stopForeground(STOP_FOREGROUND_REMOVE)
        endCallAndRestartRepository()
    }

    interface Listener {
        fun onCallReceived(model: SocketDataModel)
        fun onCallDeclined(model: SocketDataModel)
    }

    interface EndCallListener {
        fun onCallEnded()
    }
}