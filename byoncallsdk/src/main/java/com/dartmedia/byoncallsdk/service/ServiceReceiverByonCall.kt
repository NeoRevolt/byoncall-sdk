package com.dartmedia.byoncallsdk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceReceiverByonCall : BroadcastReceiver() {

    private var serviceClientByonCall: ServiceClientByonCall? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        if (serviceClientByonCall == null && context != null) {
            serviceClientByonCall = ServiceClientByonCall.instance(context)
        }
        when (intent?.action) {
            "ACTION_END_CALL" -> {
                serviceClientByonCall?.sendEndCall()
                // TODO(Zal): Do more action when notification action triggered
//                serviceRepository.stopService()
//                context?.startActivity(Intent(context,CloseActivity::class.java))
            }
        }

    }
}