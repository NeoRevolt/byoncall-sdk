package com.dartmedia.brandedsdk.utils.permission

import android.Manifest.permission.*
import android.os.Build
import androidx.annotation.RequiresApi

sealed class Permission(vararg val permissions: String) {
    // Individual permissions
    object Camera : Permission(CAMERA)

    // Bundled permissions
    object MandatoryForFeatureOne : Permission(WRITE_EXTERNAL_STORAGE, ACCESS_FINE_LOCATION)

    // Grouped permissions
    object Location : Permission(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
    object Storage : Permission(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE)

    @RequiresApi(33)
    object Notification : Permission(POST_NOTIFICATIONS)

    companion object {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun from(permission: String) = when (permission) {
            ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION -> Location
            WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE -> Storage
            POST_NOTIFICATIONS -> Notification
            CAMERA -> Camera

            else -> throw IllegalArgumentException("Unknown permission: $permission")
        }
    }
}