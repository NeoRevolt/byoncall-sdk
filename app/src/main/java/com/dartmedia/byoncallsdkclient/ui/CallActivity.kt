package com.dartmedia.byoncallsdkclient.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dartmedia.byoncallsdk.libraryapi.ByonCallSDK
import com.dartmedia.byoncallsdk.model.UserStatusEnum
import com.dartmedia.byoncallsdk.utils.audio.manager.RTCAudioManager
import com.dartmedia.byoncallsdk.utils.date.DateUtils.getCurrentDateDetailed
import com.dartmedia.byoncallsdk.utils.extension.convertToHumanTime
import com.dartmedia.byoncallsdk.utils.image.WhiteBackgroundTransformation
import com.dartmedia.byoncallsdkclient.R
import com.dartmedia.byoncallsdkclient.databinding.ActivityCallBinding
import com.dartmedia.byoncallsdkclient.ui.viewmodel.CallLogViewModel
import com.dartmedia.byoncallsdkclient.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallActivity : AppCompatActivity(), ByonCallSDK.InCallListener {

    private var byonCallSDK = ByonCallSDK.getInstance()

    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var callLogViewModel: CallLogViewModel
    private lateinit var binding: ActivityCallBinding

    private var sender: String? = null
    private var target: String? = null
    private var targetName: String? = null
    private var isVideoCall: Boolean = true
    private var isCaller: Boolean = true
    private var targetImgUrl: String? = null
    private var callMessage: String? = null

    private var isMicrophoneMuted = false
    private var isCameraMuted = false
    private var isSpeakerMode = true
    private var isScreenCasting = false

    private var callDuration: Int = 0
    private var callStartTime: String = ""

    override fun onStart() {
        super.onStart()
        requestScreenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                byonCallSDK.setScreenPermissionIntent(intent)
                isScreenCasting = true
                updateUiToScreenCaptureIsOn()
                byonCallSDK.toggleScreenShare(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        callLogViewModel = obtainViewModel(this@CallActivity)

        init()
    }

    private fun obtainViewModel(activity: AppCompatActivity): CallLogViewModel {
        val factory = ViewModelFactory.getInstance(activity.application)
        return ViewModelProvider(activity, factory).get(CallLogViewModel::class.java)
    }

    private fun init() {
        intent.getStringExtra("target")?.let {
            this.target = it
        } ?: kotlin.run {
            finish()
        }
        byonCallSDK.inCallListener = this
        targetName = intent.getStringExtra("targetName") ?: target
        targetImgUrl = intent.getStringExtra("targetImg")
        callMessage = intent.getStringExtra("message")
        sender = intent.getStringExtra("sender")
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        isCaller = intent.getBooleanExtra("isCaller", true)
        if (targetImgUrl != null) {
            Glide.with(this)
                .load(targetImgUrl)
                .transform(WhiteBackgroundTransformation())
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.rounded_message_gray)
                        .error(R.drawable.asset_person_ic)
                        .circleCrop()
                )
                .into(binding.voiceCallCallerImage)
        } else {
            Glide.with(this)
                .load(R.drawable.asset_person_ic)
                .apply(RequestOptions.circleCropTransform())
                .into(binding.voiceCallCallerImage)
        }

        target?.let { target ->
            targetName?.let { targetName ->
                byonCallSDK.observeTargetContact(target) {
                    when (it) {

                        UserStatusEnum.ONLINE -> {
                            binding.apply {
                                callTitleTv.text = targetName
                                voiceCallCallerName.text = targetName
                            }
                        }

                        UserStatusEnum.CALLING -> {
                            binding.apply {
                                //                            callTimeoutTimer.cancel()
                                callTitleTv.text = targetName
                                voiceCallCallerName.text = targetName
                            }
                        }

                        UserStatusEnum.IN_CALL -> {
                            binding.apply {
                                //                            callTimeoutTimer.cancel()
                                callTitleTv.text = targetName
                                voiceCallCallerName.text = targetName
                                callStartTime = getCurrentDateDetailed()
                                CoroutineScope(Dispatchers.IO).launch {
                                    for (duration in 0..3600) {
                                        delay(1000)
                                        withContext(Dispatchers.Main) {
                                            //convert this int to human readable time
                                            callTimerTv.text = duration.convertToHumanTime()
                                            voiceCallStatusText.text = duration.convertToHumanTime()
                                            callDuration = duration
                                        }
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        binding.apply {
            if (!isVideoCall) {
                toggleCameraButton.isVisible = false
                screenShareButton.isVisible = false
                switchCameraButton.isVisible = false
                voiceCallLayouts.isVisible = true
                voiceCallEndCallButton.setOnClickListener {
                    byonCallSDK.endCall()
                }
            }

            byonCallSDK.setupViews(
                isVideoCall = isVideoCall,
                isCaller = isCaller,
                target = target!!,
                localSurfaceView = localView,
                remoteSurfaceView = remoteView
            )

            endCallButton.setOnClickListener {
                byonCallSDK.endCall()
            }

            switchCameraButton.setOnClickListener {
                byonCallSDK.switchCamera()
            }
        }
        setupMicToggleClicked()
        setupCameraToggleClicked()
        setupToggleAudioDevice()
        setupScreenCasting()

    }

    private fun setupScreenCasting() {
        binding.apply {
            screenShareButton.setOnClickListener {
                if (!isScreenCasting) {
                    //we have to start casting
                    AlertDialog.Builder(this@CallActivity)
                        .setTitle("Screen Casting")
                        .setMessage("You sure to start casting ?")
                        .setPositiveButton("Yes") { dialog, _ ->
                            //start screen casting process
                            startScreenCapture()
                            dialog.dismiss()
                        }.setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }.create().show()
                } else {
                    // end screen casting
                    isScreenCasting = false
                    updateUiToScreenCaptureIsOff()
                    byonCallSDK.toggleScreenShare(false)
                }
            }
        }
    }

    private fun startScreenCapture() {
        val mediaProjectionManager = application.getSystemService(
            Context.MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager

        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        requestScreenCaptureLauncher.launch(captureIntent)

    }

    private fun updateUiToScreenCaptureIsOn() {
        binding.apply {
            localView.isVisible = false
            switchCameraButton.isVisible = false
            toggleCameraButton.isVisible = false
            screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
        }

    }

    private fun updateUiToScreenCaptureIsOff() {
        binding.apply {
            localView.isVisible = true
            switchCameraButton.isVisible = true
            toggleCameraButton.isVisible = true
            screenShareButton.setImageResource(R.drawable.ic_screen_share)
        }
    }

    private fun setupMicToggleClicked() {
        binding.apply {
            toggleMicrophoneButton.setOnClickListener {
                if (!isMicrophoneMuted) {
                    byonCallSDK.toggleAudio(true)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
                } else {
                    byonCallSDK.toggleAudio(false)
                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
                }
                isMicrophoneMuted = !isMicrophoneMuted
            }
            voiceCallMuteButton.setOnClickListener {
                if (!isMicrophoneMuted) {
                    byonCallSDK.toggleAudio(true)
                    voiceCallMuteButton.setImageResource(R.drawable.ic_mic_on)
                } else {
                    byonCallSDK.toggleAudio(false)
                    voiceCallMuteButton.setImageResource(R.drawable.ic_mic_off)
                }
                isMicrophoneMuted = !isMicrophoneMuted
            }
        }
    }

    private fun setupToggleAudioDevice() {
        binding.apply {
            toggleAudioDevice.setOnClickListener {
                if (isSpeakerMode) {
                    // set it to earpiece mode
                    toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
                    byonCallSDK.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)

                } else {
                    // set it to speaker mode
                    toggleAudioDevice.setImageResource(R.drawable.ic_ear)
                    byonCallSDK.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)

                }
                isSpeakerMode = !isSpeakerMode
            }
        }
    }

    private fun setupCameraToggleClicked() {
        binding.apply {
            toggleCameraButton.setOnClickListener {
                if (!isCameraMuted) {
                    byonCallSDK.toggleVideo(true)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                } else {
                    byonCallSDK.toggleVideo(false)
                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                }

                isCameraMuted = !isCameraMuted
            }
        }
    }

    override fun onCallStatusChanged(statusEnum: UserStatusEnum) {
        Log.d("CallActivity", "onCallStatusChanged : ${statusEnum.name}")
        runOnUiThread {
            when (statusEnum) {

                UserStatusEnum.CALLING -> {
                    binding.apply {
                        callStatusTv.text = "Calling..."
                        callStatusVideoTv.text = "Calling..."
                    }
                }


                UserStatusEnum.IN_CALL -> {
                    binding.apply {
                        callStatusTv.text = "In-Call"
                        callStatusVideoTv.text = "In-Call"
                    }
                }

                UserStatusEnum.FAILED -> {
                    binding.apply {
                        callStatusTv.text = "Failed"
                        callStatusVideoTv.text = "Failed"
                    }
                }

                UserStatusEnum.OFFLINE -> {
                    binding.apply {
                        callStatusTv.text = "Offline"
                        callStatusVideoTv.text = "Offline"
                    }
                }


                else -> {}
            }
        }
    }

    override fun onCallEnded() {
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        byonCallSDK.endCall()
    }

    override fun onDestroy() {
        super.onDestroy()
        byonCallSDK.clearSurfaceView()
    }
}