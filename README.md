# Air Quality Monitor 🌬️📱

An Android application that monitors live air quality for the user's current location, displays health recommendations, shows a 24-hour hourly forecast, and logs past readings using a local database.

## Key Features

- 🔐 **Firebase Authentication & Google Sign-In**: User authentication with Google Sign-In support.
- 📍 **Fused Location Provider**: Automatic location retrieval using `getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)`.
- 🌤️ **Open-Meteo Air Quality API**: Live air quality metrics including **US AQI**, **PM2.5**, **PM10**, and **Carbon Monoxide (CO)**.
- 🎨 **Dynamic Dashboard Color**: Dashboard card background changes color according to US AQI categories (*Good*, *Moderate*, *Unhealthy for Sensitive Groups*, *Unhealthy*, *Very Unhealthy*, *Hazardous*).
- 💡 **Health Advice**: Real-time health guidance based on current air quality.
- 📊 **24-Hour Forecast**: Horizontal `RecyclerView` displaying hourly AQI trends for the next 24 hours.
- 💾 **Reading Log & History (Room DB)**: Save current air quality readings to a local Room database, view saved history list, and delete entries.

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **UI Framework**: Android XML ViewBinding, Material Components 3, RecyclerView
- **Database**: Room Database
- **Networking**: Retrofit 2, Gson, OkHttp Logging Interceptor
- **Location**: Google Play Services Fused Location Provider
- **Authentication**: Firebase Authentication & Google Sign-In / Credential Manager
- **Build System**: Android Gradle Plugin 9 (AGP 9) with Gradle 9 & Version Catalog (`libs.versions.toml`)

## API Endpoint Used

```http
GET https://air-quality-api.open-meteo.com/v1/air-quality?latitude={lat}&longitude={lon}&current=us_aqi,pm2_5,pm10,carbon_monoxide&hourly=us_aqi&timezone=auto
```
