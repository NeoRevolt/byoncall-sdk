package com.dartmedia.brandedsdk.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    private var retrofit: Retrofit? = null
    private var testing: Retrofit? = null

    fun getClient(baseUrl: String): Retrofit {
        return retrofit ?: Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build().also { retrofit = it }
    }

}