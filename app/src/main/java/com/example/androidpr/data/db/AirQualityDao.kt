package com.example.androidpr.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AirQualityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AirQualityRecord): Long

    @Query("SELECT * FROM air_quality_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AirQualityRecord>>

    @Delete
    suspend fun deleteRecord(record: AirQualityRecord)
}
