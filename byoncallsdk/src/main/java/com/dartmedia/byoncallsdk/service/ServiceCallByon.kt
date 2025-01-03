package com.dartmedia.byoncallsdk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dartmedia.byoncallsdk.R
import com.dartmedia.byoncallsdk.model.SocketDataModel
import com.dartmedia.byoncallsdk.model.SocketDataTypeEnum
import com.dartmedia.byoncallsdk.model.UserStatusEnum
import com.dartmedia.byoncallsdk.model.isValid
import com.dartmedia.byoncallsdk.repository.ByonCallRepository
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.END_CALL
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.SETUP_VIEWS
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.START_SERVICE
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.STOP_SERVICE
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.SWITCH_CAMERA
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.TOGGLE_AUDIO
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.TOGGLE_AUDIO_DEVICE
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.TOGGLE_SCREEN_SHARE
import com.dartmedia.byoncallsdk.service.ServiceActionsEnum.TOGGLE_VIDEO
import com.dartmedia.byoncallsdk.utils.audio.manager.RTCAudioManager
import org.webrtc.SurfaceViewRenderer


class ServiceCallByon : Service(), ByonCallRepository.Listener {

    private var isServiceRunning = false
    private var username: String? = null

    private val byonCallRepository by lazy { ByonCallRepository.instance(this) }

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
        byonCallRepository.endCall()
        byonCallRepository.disconnectSocket {
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
                byonCallRepository.toggleVideo(true)
            }
            byonCallRepository.setScreenCaptureIntent(screenPermissionIntent!!)
            byonCallRepository.toggleScreenShare(true)

        } else {
            // Stop Share Screen and check if camera streaming was on so make it on back again
            byonCallRepository.toggleScreenShare(false)
            if (isPreviousCallStateVideo) {
                byonCallRepository.toggleVideo(false)
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
        byonCallRepository.toggleVideo(shouldBeMuted)
    }

    private fun handleToggleAudio(incomingIntent: Intent) {
        val shouldBeMuted = incomingIntent.getBooleanExtra("shouldBeMuted", true)
        byonCallRepository.toggleAudio(shouldBeMuted)
    }

    private fun handleSwitchCamera() {
        byonCallRepository.switchCamera()
    }

    private fun handleEndCall() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        //1. we have to send a signal to other peer that call is ended
        byonCallRepository.sendEndCall()
        //2.end out call process and restart our webrtc client
        endCallAndRestartRepository()
    }

    private fun endCallAndRestartRepository() {
        byonCallRepository.endCall()
        inCallListener?.onCallEnded()
        username?.let { username ->
            byonCallRepository.initWebrtcClient(username)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun handleSetupViews(incomingIntent: Intent) {
        startServiceWithNotification()
        val isCaller = incomingIntent.getBooleanExtra("isCaller", false)
        val isVideoCall = incomingIntent.getBooleanExtra("isVideoCall", true)
        val target = incomingIntent.getStringExtra("target")
        this.isPreviousCallStateVideo = isVideoCall
        byonCallRepository.setTarget(target!!)
        byonCallRepository.initLocalSurfaceView(localSurfaceView!!, isVideoCall)
        byonCallRepository.initRemoteSurfaceView(remoteSurfaceView!!)

        if (!isCaller) {
            // start the video call
            byonCallRepository.startCall()
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
            byonCallRepository.listener = this
            byonCallRepository.observeSocket()
            byonCallRepository.initWebrtcClient(username!!)

        } else {
            Log.d(TAG, "handleStartService : Service is already running")
        }
    }

    private fun startServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "channel1", "foreground", NotificationManager.IMPORTANCE_DEFAULT
            )

            val intent = Intent(this, ServiceReceiverByonCall::class.java).apply {
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