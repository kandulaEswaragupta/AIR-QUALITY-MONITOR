package com.example.androidpr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpr.data.api.AirQualityResponse
import com.example.androidpr.data.db.AirQualityRecord
import com.example.androidpr.data.repository.AirQualityRepository
import com.example.androidpr.ui.util.AqiUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class MainViewModel(
    private val repository: AirQualityRepository
) : ViewModel() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _airQualityState = MutableStateFlow<UiState<AirQualityResponse>>(UiState.Loading)
    val airQualityState: StateFlow<UiState<AirQualityResponse>> = _airQualityState.asStateFlow()

    private val _historyState = MutableStateFlow<List<AirQualityRecord>>(emptyList())
    val historyState: StateFlow<List<AirQualityRecord>> = _historyState.asStateFlow()

    private val _locationName = MutableStateFlow("Lat: 12.97, Lon: 77.59")
    val locationName: StateFlow<String> = _locationName.asStateFlow()

    private var currentLat: Double = 12.9716
    private var currentLon: Double = 77.5946

    init {
        checkUserStatus()
        observeHistory()
    }

    fun checkUserStatus() {
        try {
            _currentUser.value = auth.currentUser
        } catch (e: Exception) {
            _currentUser.value = null
        }
    }

    fun updateLocation(lat: Double, lon: Double, displayName: String? = null) {
        currentLat = lat
        currentLon = lon
        _locationName.value = displayName ?: String.format(Locale.US, "Lat: %.4f, Lon: %.4f", lat, lon)
        fetchAirQuality()
    }

    fun fetchAirQuality() {
        viewModelScope.launch {
            _airQualityState.value = UiState.Loading
            val result = repository.fetchAirQuality(currentLat, currentLon)
            result.onSuccess { response ->
                _airQualityState.value = UiState.Success(response)
            }.onFailure { error ->
                _airQualityState.value = UiState.Error(error.localizedMessage ?: "Failed to fetch air quality data")
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.getAllRecords().collectLatest { records ->
                _historyState.value = records
            }
        }
    }

    fun saveCurrentReading() {
        val currentState = _airQualityState.value
        if (currentState is UiState.Success) {
            val currentAQI = currentState.data.current?.usAqi ?: return
            val pm25 = currentState.data.current.pm25 ?: 0.0
            val pm10 = currentState.data.current.pm10 ?: 0.0
            val co = currentState.data.current.carbonMonoxide ?: 0.0
            val category = AqiUtils.getCategory(currentAQI).label

            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date())

            val record = AirQualityRecord(
                timestamp = System.currentTimeMillis(),
                dateFormatted = dateStr,
                aqi = currentAQI,
                category = category,
                location = _locationName.value,
                pm25 = pm25,
                pm10 = pm10,
                co = co
            )

            viewModelScope.launch {
                repository.saveRecord(record)
            }
        }
    }

    fun deleteRecord(record: AirQualityRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
    }
}
