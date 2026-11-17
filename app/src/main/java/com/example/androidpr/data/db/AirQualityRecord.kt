package com.example.androidpr.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "air_quality_records")
data class AirQualityRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val dateFormatted: String,
    val aqi: Int,
    val category: String,
    val location: String,
    val pm25: Double,
    val pm10: Double,
    val co: Double
)
