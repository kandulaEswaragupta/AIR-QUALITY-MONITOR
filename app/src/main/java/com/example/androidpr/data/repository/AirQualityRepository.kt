package com.example.androidpr.data.repository

import com.example.androidpr.data.api.AirQualityApiService
import com.example.androidpr.data.api.AirQualityResponse
import com.example.androidpr.data.db.AirQualityDao
import com.example.androidpr.data.db.AirQualityRecord
import kotlinx.coroutines.flow.Flow

class AirQualityRepository(
    private val apiService: AirQualityApiService,
    private val dao: AirQualityDao
) {
    suspend fun fetchAirQuality(latitude: Double, longitude: Double): Result<AirQualityResponse> {
        return try {
            val response = apiService.getAirQuality(latitude, longitude)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllRecords(): Flow<List<AirQualityRecord>> {
        return dao.getAllRecords()
    }

    suspend fun saveRecord(record: AirQualityRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun deleteRecord(record: AirQualityRecord) {
        dao.deleteRecord(record)
    }
}
