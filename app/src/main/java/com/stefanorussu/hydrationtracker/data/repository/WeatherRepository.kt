package com.stefanorussu.hydrationtracker.data.repository

import com.stefanorussu.hydrationtracker.data.network.OpenMeteoApi

class WeatherRepository {
    private val api = OpenMeteoApi.create()

    // Coordinate di Sassari di default
    suspend fun getSassariTemperature(lat: Double = 40.7272, lon: Double = 8.5616): Double? {
        return try {
            val response = api.getCurrentWeather(lat, lon)
            android.util.Log.d("METEO_SYNC", "Temperatura a Sassari: ${response.current_weather.temperature}°C")
            response.current_weather.temperature
        } catch (e: Exception) {
            android.util.Log.e("METEO_SYNC", "Impossibile scaricare il meteo: ${e.message}")
            null
        }
    }
}