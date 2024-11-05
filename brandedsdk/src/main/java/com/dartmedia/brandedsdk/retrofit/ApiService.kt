package com.dartmedia.brandedsdk.retrofit

import android.webkit.PermissionRequest
import retrofit2.Call
import retrofit2.http.POST

data class PermissionReportRequest(
    val phone: String,
    val permissionStatus: String
)

data class PermissionReportResponse(
    val code: Int,
    val message: String
)

interface ApiService {
    @POST("reportPermission")
    fun reportPermissionStatus(
        request: PermissionReportRequest
    ): Call<PermissionReportResponse>
}