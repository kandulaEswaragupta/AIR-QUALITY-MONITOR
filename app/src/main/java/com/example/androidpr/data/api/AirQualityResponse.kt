package com.example.androidpr.data.api

import com.google.gson.annotations.SerializedName

data class AirQualityResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("current") val current: CurrentAirQuality?,
    @SerializedName("hourly") val hourly: HourlyAirQuality?
)

data class CurrentAirQuality(
    @SerializedName("time") val time: String?,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("carbon_monoxide") val carbonMonoxide: Double?
)

data class HourlyAirQuality(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("us_aqi") val usAqi: List<Int?>?
)
