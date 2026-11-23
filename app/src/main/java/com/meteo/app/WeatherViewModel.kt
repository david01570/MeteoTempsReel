package com.meteo.app

import android.app.Application
import android.location.Geocoder
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val wind: Double,
    val precipitations: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val city: String,
    val tempMin: Double?,
    val tempMax: Double?
)

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Error(val message: String) : WeatherUiState
    data class Success(val weather: CurrentWeather) : WeatherUiState
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    val state = mutableStateOf<WeatherUiState>(WeatherUiState.Loading)

    fun requestLocation() {
        state.value = WeatherUiState.Loading
        LocationProvider.getLocation(
            context = getApplication(),
            onSuccess = { location -> loadWeather(location.latitude, location.longitude) },
            onError = { message -> state.value = WeatherUiState.Error(message) }
        )
    }

    fun loadWeather(latitude: Double, longitude: Double) {
        state.value = WeatherUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val weather = fetchWeather(latitude, longitude)
                val city = resolveCity(latitude, longitude)
                withContext(Dispatchers.Main) {
                    state.value = WeatherUiState.Success(weather.copy(city = city))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    state.value = WeatherUiState.Error(
                        e.message ?: "Erreur inconnue lors de la récupération de la météo."
                    )
                }
            }
        }
    }

    private fun fetchWeather(latitude: Double, longitude: Double): CurrentWeather {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day," +
                "precipitation,weather_code,wind_speed_10m" +
                "&daily=temperature_2m_max,temperature_2m_min,weather_code" +
                "&forecast_days=1&timezone=auto"
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return parseWeather(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseWeather(json: String): CurrentWeather {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")
        val tempMax = daily?.optJSONArray("temperature_2m_max")?.optDouble(0)
        val tempMin = daily?.optJSONArray("temperature_2m_min")?.optDouble(0)
        return CurrentWeather(
            temperature = current.getDouble("temperature_2m"),
            feelsLike = current.getDouble("apparent_temperature"),
            humidity = current.optInt("relative_humidity_2m"),
            wind = current.optDouble("wind_speed_10m", 0.0),
            precipitations = current.optDouble("precipitation", 0.0),
            weatherCode = current.optInt("weather_code", 0),
            isDay = current.optInt("is_day", 1) == 1,
            city = "",
            tempMin = tempMin?.takeUnless { it.isNaN() },
            tempMax = tempMax?.takeUnless { it.isNaN() }
        )
    }

    private fun resolveCity(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(getApplication())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull()
            when {
                address?.locality != null -> address.locality
                address?.subAdminArea != null -> address.subAdminArea
                address?.adminArea != null -> address.adminArea
                else -> String.format("%.2f°, %.2f°", latitude, longitude)
            }
        } catch (_: Exception) {
            String.format("%.2f°, %.2f°", latitude, longitude)
        }
    }
}
