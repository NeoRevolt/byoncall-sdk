package com.dartmedia.byoncallsdk.service

import android.content.Context
import android.content.Intent

class ServiceClientByonCall(
    private val context: Context
) {

    companion object {
        fun instance(context: Context): ServiceClientByonCall {
            return ServiceClientByonCall(context)
        }
    }

    fun startService(username: String) {
        Thread {
            val intent = Intent(context, ServiceCallByon::class.java)
            intent.putExtra("username", username)
            intent.action = ServiceActionsEnum.START_SERVICE.name
            startServiceIntent(intent)
        }.start()
    }

    private fun startServiceIntent(intent: Intent) {
        context.startService(intent) //  Start serive in background
    }

    fun setupViews(videoCall: Boolean, caller: Boolean, target: String) {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.apply {
            action = ServiceActionsEnum.SETUP_VIEWS.name
            putExtra("isVideoCall", videoCall)
            putExtra("target", target)
            putExtra("isCaller", caller)
        }
        startServiceIntent(intent)
    }

    fun sendEndCall() {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.END_CALL.name
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.TOGGLE_AUDIO.name
        intent.putExtra("shouldBeMuted", shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.TOGGLE_VIDEO.name
        intent.putExtra("shouldBeMuted", shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.TOGGLE_AUDIO_DEVICE.name
        intent.putExtra("type", type)
        startServiceIntent(intent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.TOGGLE_SCREEN_SHARE.name
        intent.putExtra("isStarting", isStarting)
        startServiceIntent(intent)
    }

    fun stopService() {
        val intent = Intent(context, ServiceCallByon::class.java)
        intent.action = ServiceActionsEnum.STOP_SERVICE.name
        startServiceIntent(intent)
    }

}