package com.dartmedia.byoncallsdkclient.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query


data class BrandingRegisterModel(
    val name: String,
    val phonenumber: String,
    val email: String,
    val image: String
)

data class CallHistoryResponse(
    val code: Int,
    val data: List<CallHistoryData>
)

data class CallHistoryData(
    val id: Int,
    val call_id: String,
    val nama_operator: String,
    val phonenumber: String,
    val target_phonenumber: String,
    val name: String,
    val recording: String?,
    val image_url: String,
    val intent: String,
    val status: String,
    val team_id: String,
    val duration: Int,
    val on_progress: Int,
    val date: String,
    val updated_at: String
)

interface ApiService {

    @POST("api/branded-call-register")
    fun registerBrand(
        @Header("Authorization")
        authorization: String,

        @Body
        body: BrandingRegisterModel
    ): Call<Void>


    @GET("api/branded-call/history")
    fun getCallHistoryById(
        @Header("Authorization")
        authorization: String,

        @Query("phonenumber")
        phonenumber: String
    ): Call<CallHistoryResponse>
}