package com.dartmedia.brandedsdk.utils.phone

import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber

object PhoneNumberUtils {

    fun isValidPhoneNumber(phoneNumber: String, countryCode: String): Boolean {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, countryCode)
            phoneUtil.isValidNumber(numberProto) // returns true or false
        } catch (e: Exception) {
            Log.e("PhoneNumberUtils", "Exception: $e")
            false
        }
    }

    fun formatPhoneNumber(phoneNumber: String, countryCode: String): String {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, countryCode)
            phoneUtil.format(
                numberProto,
                PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
            ) // formats the number
        } catch (e: Exception) {
            Log.e("PhoneNumberUtils", "Exception: $e")
            phoneNumber
        }
    }

    fun removeLeadingZero(phoneNumber: String): String {
        return if (phoneNumber.startsWith("0")) {
            phoneNumber.substring(1)  // Remove the first character (the "0")
        } else {
            phoneNumber
        }
    }
}