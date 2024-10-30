package com.dartmedia.brandedsdk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MainServiceReceiver : BroadcastReceiver() {

    private var mainServiceRepository: MainServiceRepository? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        if (mainServiceRepository == null && context != null) {
            mainServiceRepository = MainServiceRepository.instance(context)
        }
        when (intent?.action) {
            "ACTION_END_CALL" -> {
                mainServiceRepository?.sendEndCall()
                // TODO(Zal): Do more action when notification action triggered
//                serviceRepository.stopService()
//                context?.startActivity(Intent(context,CloseActivity::class.java))
            }
        }

    }
}