package com.dartmedia.brandedsdk.sip

import android.net.sip.SipProfile

object SipConfig {
    fun createSipProfile(): SipProfile {
        return SipProfile.Builder(
            Account.USERNAME,
            Account.SERVER,
        )
            .setPassword(Account.PASSWORD)
            .setPort(Account.PORT)
            .build()
    }


    object Account {
        const val USERNAME = "200"
        const val SERVER = "dmdev.byonchat2.com"
        const val PORT = 2120
        const val PASSWORD = "secret!@123k"
    }
}

