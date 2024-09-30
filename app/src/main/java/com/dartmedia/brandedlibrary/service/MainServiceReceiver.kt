package com.dartmedia.brandedlibrary.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainServiceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "ACTION_END_CALL" -> {
                mainServiceRepository.sendEndCall()
                // TODO(Zal): Do more action when notification action triggered
//                serviceRepository.stopService()
//                context?.startActivity(Intent(context,CloseActivity::class.java))
            }
        }

    }
}