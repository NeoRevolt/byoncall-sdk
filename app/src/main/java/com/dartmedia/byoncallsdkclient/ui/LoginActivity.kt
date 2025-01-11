package com.dartmedia.byoncallsdkclient.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dartmedia.byoncallsdkclient.databinding.ActivityLoginBinding
import com.dartmedia.byoncallsdk.utils.contacts.ContactSaver
import com.dartmedia.byoncallsdk.utils.extension.getCameraAndMicPermission
import com.dartmedia.byoncallsdk.utils.phone.PhoneNumberUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {

    companion object {
        private val TAG = LoginActivity::class.java.simpleName.toString()
    }

    private lateinit var binding: ActivityLoginBinding
    private val contactSaver by lazy { ContactSaver.instance(this) }
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {


            btnProceed.setOnClickListener {
                val countryCode = countryCodePicker.selectedCountryCodeWithPlus
                val phoneNumber = PhoneNumberUtils.removeLeadingZero(etMyPhone.text.toString())
                val phoneNumberFull = "${countryCode}${phoneNumber}"

                Log.d(TAG, "phoneNumberFull : $phoneNumberFull")

                if (phoneNumberFull.isNotEmpty() && PhoneNumberUtils.isValidPhoneNumber(
                        phoneNumberFull,
                        countryCode
                    )
                ) {
                    //TODO
                    getCameraAndMicPermission { allAllowed ->
                        if (allAllowed) {
                            scope.launch {
                                contactSaver.saveContactInfo(
                                    displayName = "DartMedia",
                                    phoneNumber = "+6281905598599",
                                    imageUrl = "https://dartmedia.co.id/images/logo_dartmedia.png"
                                )
                            }
                            val intent =
                                Intent(this@LoginActivity, HistoryCallActivity::class.java).apply {
                                    putExtra(HistoryCallActivity.MY_PHONE, phoneNumberFull)
                                }
                            startActivity(intent)

//                            val intent =
//                                Intent(this@LoginActivity, ChatActivity::class.java).apply {
//                                    putExtra(ChatActivity.MY_PHONE, phoneNumberFull)
//                                    putExtra(ChatActivity.TARGET_PHONE, "+6281905598577")
//                                    putExtra(ChatActivity.MY_BRAND_NAME, "Client")
//                                    putExtra(ChatActivity.MY_IMAGE_URL, "https://dartmedia.co.id/images/logo_dartmedia.png")
//                                    putExtra(ChatActivity.CALL_MESSAGE, "Client Test")
//                                }
//                            startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Invalid phone number",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()
        binding.etMyPhone.requestFocus()
    }
}