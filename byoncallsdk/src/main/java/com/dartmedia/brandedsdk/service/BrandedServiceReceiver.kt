package com.dartmedia.brandedsdk.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BrandedServiceReceiver : BroadcastReceiver() {

    private var brandedServiceClient: BrandedServiceClient? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        if (brandedServiceClient == null && context != null) {
            brandedServiceClient = BrandedServiceClient.instance(context)
        }
        when (intent?.action) {
            "ACTION_END_CALL" -> {
                brandedServiceClient?.sendEndCall()
                // TODO(Zal): Do more action when notification action triggered
//                serviceRepository.stopService()
//                context?.startActivity(Intent(context,CloseActivity::class.java))
            }
        }

    }
}