package com.dartmedia.brandedlibraryclient.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dartmedia.brandedlibraryclient.databinding.ActivityLoginBinding
import com.dartmedia.brandedsdk.utils.phone.PhoneNumberUtils
import com.dartmedia.brandedsdk.utils.extension.getCameraAndMicPermission

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

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
                    getCameraAndMicPermission {
                        val intent =
                            Intent(this@LoginActivity, HistoryCallActivity::class.java).apply {
                                putExtra(HistoryCallActivity.MY_PHONE, phoneNumberFull)
                            }
                        startActivity(intent)
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

//            // Set the selected item (so that it shows as active)
//            bottomNavigation.selectedItemId = R.id.navigation_login
//
//            // Handle navigation item clicks
//            bottomNavigation.setOnNavigationItemSelectedListener { item ->
//                when (item.itemId) {
//                    R.id.navigation_login -> {
//                        true
//                    }
//
//                    R.id.navigation_history -> {
//                        startActivity(Intent(this@LoginActivity, HistoryCallActivity::class.java))
//                        overridePendingTransition(0, 0)
//                        true
//                    }
//
//                    else -> false
//                }
//            }
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