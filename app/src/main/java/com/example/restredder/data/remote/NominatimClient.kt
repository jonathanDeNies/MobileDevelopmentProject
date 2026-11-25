package com.example.restredder.data.remote

import com.example.restredder.AppInfo
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NominatimClient {

    private const val BASE_URL = "https://nominatim.openstreetmap.org/"

    val api: NominatimApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", AppInfo.USER_AGENT)
                        .build()
                )
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}
