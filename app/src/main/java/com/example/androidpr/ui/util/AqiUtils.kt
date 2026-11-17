package com.example.androidpr.ui.util

import com.example.androidpr.R

object AqiUtils {

    enum class Category(val label: String) {
        GOOD("Good"),
        MODERATE("Moderate"),
        UNHEALTHY_FOR_SENSITIVE("Unhealthy for Sensitive Groups"),
        UNHEALTHY("Unhealthy"),
        VERY_UNHEALTHY("Very Unhealthy"),
        HAZARDOUS("Hazardous")
    }

    fun getCategory(aqi: Int): Category {
        return when {
            aqi <= 50 -> Category.GOOD
            aqi <= 100 -> Category.MODERATE
            aqi <= 150 -> Category.UNHEALTHY_FOR_SENSITIVE
            aqi <= 200 -> Category.UNHEALTHY
            aqi <= 300 -> Category.VERY_UNHEALTHY
            else -> Category.HAZARDOUS
        }
    }

    fun getColorResId(aqi: Int): Int {
        return when (getCategory(aqi)) {
            Category.GOOD -> R.color.aqi_good
            Category.MODERATE -> R.color.aqi_moderate
            Category.UNHEALTHY_FOR_SENSITIVE -> R.color.aqi_usg
            Category.UNHEALTHY -> R.color.aqi_unhealthy
            Category.VERY_UNHEALTHY -> R.color.aqi_very_unhealthy
            Category.HAZARDOUS -> R.color.aqi_hazardous
        }
    }

    fun getHealthAdvice(aqi: Int): String {
        return when (getCategory(aqi)) {
            Category.GOOD ->
                "Air quality is satisfactory, and air pollution poses little or no risk. Enjoy outdoor activities!"
            Category.MODERATE ->
                "Air quality is acceptable. However, sensitive individuals (asthma/elderly) should consider limiting prolonged outdoor exertion."
            Category.UNHEALTHY_FOR_SENSITIVE ->
                "Members of sensitive groups may experience health effects. The general public is less likely to be affected. Take precautions!"
            Category.UNHEALTHY ->
                "Everyone may begin to experience health effects. Wear an N95 mask outdoors and reduce heavy outdoor exertion."
            Category.VERY_UNHEALTHY ->
                "Health alert: risk of health effects is increased for everyone. Avoid outdoor exertion and keep indoor air clean."
            Category.HAZARDOUS ->
                "Health warning of emergency conditions! The entire population is likely to be affected. Stay indoors and use air purifiers."
        }
    }
}
