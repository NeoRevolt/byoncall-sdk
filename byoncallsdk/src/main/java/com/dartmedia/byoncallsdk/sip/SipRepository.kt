package com.dartmedia.byoncallsdk.sip

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.sip.SipAudioCall
import android.net.sip.SipAudioCall.Listener
import android.net.sip.SipManager
import android.net.sip.SipProfile
import android.net.sip.SipRegistrationListener
import android.util.Log


class SipRepository(private val context: Context) {

    var sipListener: SIPListener? = null

    private var sipManager: SipManager? = null
    private var sipProfile: SipProfile? = null

    fun initSIP(context: Context) {
        if (SipManager.isVoipSupported(context) && SipManager.isApiSupported(context)) {
            sipManager = SipManager.newInstance(context)


            try {
                sipProfile = SipConfig.createSipProfile()

                // Register with SIP server
                sipManager?.register(sipProfile, 30, object : SipRegistrationListener {
                    override fun onRegistering(localProfileUri: String?) {
                        Log.d(TAG, "Registering with SIP server : $localProfileUri")
                    }

                    override fun onRegistrationDone(localProfileUri: String?, expiryTime: Long) {
                        Log.d(
                            TAG,
                            "Registration successful : $localProfileUri, expiryTime: $expiryTime"
                        )
                    }

                    override fun onRegistrationFailed(
                        localProfileUri: String?,
                        expiryTime: Int,
                        errorMessage: String?
                    ) {
                        Log.e(
                            TAG,
                            "Registration failed : $errorMessage, $localProfileUri, expiryTime: $expiryTime"
                        )
                    }
                })

            } catch (e: Exception) {
                Log.e(TAG, "Error initializing SIP : $e")
            }
        } else {
            Log.e(TAG, "SIP API is not supported on this device")
        }
    }

    fun makeCall(callee: String) {
        try {
            val listener = object : Listener() {
                override fun onReadyToCall(call: SipAudioCall?) {
                    Log.d(TAG, "Ready to call")
                }

                override fun onCalling(call: SipAudioCall?) {
                    Log.d(TAG, "On Calling...")
                }

                override fun onRinging(call: SipAudioCall?, caller: SipProfile?) {
                    sipListener?.onRinging()
                    Log.d(TAG, "On Ringing...")
                }

                override fun onRingingBack(call: SipAudioCall?) {
                    Log.d(TAG, "On Ringing Back...")
                }

                override fun onCallEstablished(call: SipAudioCall?) {
                    call?.startAudio()
                    call?.setSpeakerMode(true)
                    sipListener?.onCallEstablished()
                    Log.d(TAG, "Call Established !")
                }

                override fun onCallEnded(call: SipAudioCall?) {
                    sipListener?.onCallEnded()
                    Log.e(TAG, "Call Ended...")
                }

                override fun onCallBusy(call: SipAudioCall?) {
                    Log.e(TAG, "Call Busy")
                }

                override fun onCallHeld(call: SipAudioCall?) {
                    Log.e(TAG, "Call Held")
                }

                override fun onError(call: SipAudioCall?, errorCode: Int, errorMessage: String?) {
                    sipListener?.onError(errorCode, errorMessage)
                    Log.e(
                        TAG,
                        "Call Error : $call, errorMessage: $errorMessage, errorCode: $errorCode"
                    )
                }

                override fun onChanged(call: SipAudioCall?) {
                    super.onChanged(call)
                    Log.i(TAG, "On Changed...")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error making call $e")
        }
    }

    fun listenForIncomingCalls() {
        try {
            val intent =
                PendingIntent.getBroadcast(context, 0, Intent("android.SIP.INCOMING_CALL"), 0)
            sipManager?.open(sipProfile, intent, null)

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        sipListener?.onReceived(sipManager, intent)
//                        val incomingCall = sipManager?.takeAudioCall(intent, null)
//                        incomingCall?.startAudio()
//                        incomingCall?.setSpeakerMode(true)
                        Log.d(TAG, "Incoming call answered")

                    } catch (e: Exception) {
                        Log.e(TAG, "Error receiving call : $e")
                    }
                }

            }
            context.registerReceiver(receiver, IntentFilter("android.SIP.INCOMING_CALL"))
        } catch (e: Exception) {
            Log.e(TAG, "Error listening for incoming calls : $e")
        }
    }

    fun closeSIP() {
        try {
            sipManager?.close(sipProfile?.uriString)
            Log.e(TAG, "SIP closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing SIP: ${e.message}")
        }
    }

    interface SIPListener {
        fun onReceived(sipManager: SipManager?, intent: Intent?)
        fun onRinging()
        fun onCallEstablished()
        fun onCallEnded()
        fun onError(errorCode: Int, errorMessage: String?)
    }


    companion object {
        const val TAG = "SipRepository"
    }
}