package com.dartmedia.brandedsdk.service

import android.content.Context
import android.content.Intent

class MainServiceRepository(
    private val context: Context
) {

    companion object {
        fun instance(context: Context): MainServiceRepository {
            return MainServiceRepository(context)
        }
    }

    fun startService(username: String) {
        Thread {
            val intent = Intent(context, MainService::class.java)
            intent.putExtra("username", username)
            intent.action = MainServiceActionsEnum.START_SERVICE.name
            startServiceIntent(intent)
        }.start()
    }

    private fun startServiceIntent(intent: Intent) {
        context.startService(intent) //  Start serive in background

        //TODO(Zal): Handle service in foreground or background
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            context.startForegroundService(intent)
//        }else{
//            context.startService(intent)
//        }
    }

    fun setupViews(videoCall: Boolean, caller: Boolean, target: String) {
        val intent = Intent(context, MainService::class.java)
        intent.apply {
            action = MainServiceActionsEnum.SETUP_VIEWS.name
            putExtra("isVideoCall", videoCall)
            putExtra("target", target)
            putExtra("isCaller", caller)
        }
        startServiceIntent(intent)
    }

    fun sendEndCall() {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.END_CALL.name
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.TOGGLE_AUDIO.name
        intent.putExtra("shouldBeMuted", shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.TOGGLE_VIDEO.name
        intent.putExtra("shouldBeMuted", shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.TOGGLE_AUDIO_DEVICE.name
        intent.putExtra("type", type)
        startServiceIntent(intent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.TOGGLE_SCREEN_SHARE.name
        intent.putExtra("isStarting", isStarting)
        startServiceIntent(intent)
    }

    fun stopService() {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActionsEnum.STOP_SERVICE.name
        startServiceIntent(intent)
    }

}