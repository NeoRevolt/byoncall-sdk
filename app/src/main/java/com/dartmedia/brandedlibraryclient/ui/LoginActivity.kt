package com.dartmedia.brandedlibraryclient.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dartmedia.brandedlibraryclient.databinding.ActivityLoginBinding
import com.dartmedia.brandedsdk.utils.contacts.ContactSaver
import com.dartmedia.brandedsdk.utils.extension.getCameraAndMicPermission
import com.dartmedia.brandedsdk.utils.phone.PhoneNumberUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    @Inject
    lateinit var contactSaver: ContactSaver

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

                Log.d("LoginActivity", "phoneNumberFull : ${phoneNumberFull}")

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

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onResume() {
        super.onResume()
        binding.etMyPhone.requestFocus()
    }
}