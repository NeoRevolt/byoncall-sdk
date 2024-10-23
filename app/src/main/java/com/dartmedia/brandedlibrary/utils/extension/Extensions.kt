package com.dartmedia.brandedlibrary.utils.extension

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX

fun AppCompatActivity.getCameraAndMicPermission(success: () -> Unit) {
    PermissionX.init(this)
        .permissions(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS
        )
        .request { allGranted, _, _ ->

            if (allGranted) {
                success()
            } else {
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