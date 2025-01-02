package com.dartmedia.brandedsdk.reporting

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.dartmedia.brandedsdk.retrofit.ApiClient
import com.dartmedia.brandedsdk.retrofit.ApiService
import com.dartmedia.brandedsdk.retrofit.PermissionReportRequest
import com.dartmedia.brandedsdk.retrofit.PermissionReportResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


/**
 *
 * ReportPermissionAccess
 *
 * TODO : only test when API ready
 *
 * Check if permission is allowed
 * if not, then send report to the Server (baseUrl) with the phone number
 *
 * always run this checking when user first install the app & when login
 *
 * */

class ReportPermissionAccess(
    private val context: Context,
    private val phoneNumber: String,
    private val baseUrl: String
) {

    private val apiService: ApiService

    init {
        val retrofit = ApiClient().getClient(baseUrl)
        apiService = retrofit.create(ApiService::class.java)
    }

    fun checkAndReportPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            reportPermissionDenied()
        }
    }

    private fun reportPermissionDenied() {
        CoroutineScope(Dispatchers.IO).launch {
            val request = PermissionReportRequest(
                phone = phoneNumber,
                permissionStatus = "Denied"
            )
            try {
                apiService.reportPermissionStatus(request)
                    .enqueue(object : Callback<PermissionReportResponse> {
                        override fun onResponse(
                            call: Call<PermissionReportResponse>,
                            response: Response<PermissionReportResponse>
                        ) {
                            Log.d("PermissionHandler", "Report sent successfully")

                        }

                        override fun onFailure(call: Call<PermissionReportResponse>, t: Throwable) {
                            Log.e("PermissionHandler", "Report sent failed")
                        }

                    })
            } catch (e: Exception) {
                Log.e("PermissionHandler", "Exception in reporting: ${e.message}")
            }
        }
    }
}
