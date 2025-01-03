package com.dartmedia.byoncallsdk.utils.extension

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX

fun AppCompatActivity.getCameraAndMicPermission(success: (Boolean) -> Unit) {
    PermissionX.init(this)
        .permissions(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.USE_SIP
        )
        .request { allGranted, _, deniedList ->

            if (allGranted) {
                success(true)
                Log.d("PermissionX", "All permission granted")
            } else if (deniedList.contains(android.Manifest.permission.WRITE_CONTACTS) || deniedList.contains(
                    android.Manifest.permission.READ_CONTACTS
                )
            ) {
                success(false)
                Log.e("PermissionX", "Contacts permission denied")
                Toast.makeText(
                    this,
                    "Contact permission is required",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                success(false)
                Log.e("PermissionX", "Permission denied")
                Toast.makeText(
                    this,
                    "Camera, Mic and Storage permission is required",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
}

fun Int.convertToHumanTime(): String {
    val seconds = this % 60
    val minutes = this / 60
    val secondsString = if (seconds < 10) "0$seconds" else "$seconds"
    val minutesString = if (minutes < 10) "0$minutes" else "$minutes"
    return "$minutesString:$secondsString"
}

fun Int.convertToHumanTimeWithHours(): String {
    val seconds = this % 60
    val minutes = this / 60
    val hours = this / 3600
    val secondsString = if (seconds < 10) "0$seconds" else "$seconds"
    val minutesString =
        if (minutes < 10) "0$minutes" else if (minutes >= 60) "0${minutes % 60}" else "$minutes"
    val hoursString = if (hours < 10) "0$hours" else "$hours"
    return "$hoursString:$minutesString:$secondsString"
}

fun Int.convertToTextStyleMinutes(): String {
    val seconds = this % 60
    val minutes = this / 60
    val secondsString = if (seconds < 10) "$seconds" else "$seconds"
    val minutesString = if (minutes < 10) "$minutes" else "$minutes"
    return "$minutesString menit $secondsString detik"
}
