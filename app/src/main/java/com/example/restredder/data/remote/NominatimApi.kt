package com.example.restredder.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("accept-language") language: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countryCodes: String = "be"
    ): List<NominatimPlace>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("accept-language") language: String,
        @Query("zoom") zoom: Int = 18,
        @Query("format") format: String = "jsonv2"
    ): NominatimPlace
}

// Fields are nullable: Nominatim returns {"error": ...} when nothing is found.
data class NominatimPlace(
    val lat: String? = null,
    val lon: String? = null,
    @SerializedName("display_name") val displayName: String? = null
)
