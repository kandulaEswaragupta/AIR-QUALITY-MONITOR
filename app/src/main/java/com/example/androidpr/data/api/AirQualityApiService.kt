package com.example.androidpr.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AirQualityApiService {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "us_aqi,pm2_5,pm10,carbon_monoxide",
        @Query("hourly") hourly: String = "us_aqi",
        @Query("timezone") timezone: String = "auto"
    ): Response<AirQualityResponse>
}
