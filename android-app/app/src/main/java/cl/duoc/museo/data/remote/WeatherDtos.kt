package cl.duoc.museo.data.remote

import com.google.gson.annotations.SerializedName

data class ForecastDto(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timezone: String? = null,
    val current: CurrentWeatherDto? = null,
    val daily: DailyWeatherDto? = null
)

data class CurrentWeatherDto(
    val time: String? = null,
    @SerializedName("temperature_2m") val temperature: Double = 0.0,
    @SerializedName("relative_humidity_2m") val humidity: Int = 0,
    @SerializedName("apparent_temperature") val apparentTemperature: Double = 0.0,
    @SerializedName("is_day") val isDay: Int = 1,
    @SerializedName("weather_code") val weatherCode: Int = 0,
    @SerializedName("wind_speed_10m") val windSpeed: Double = 0.0
)

data class DailyWeatherDto(
    val time: List<String> = emptyList(),
    @SerializedName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerializedName("temperature_2m_max") val maxTemperature: List<Double> = emptyList(),
    @SerializedName("temperature_2m_min") val minTemperature: List<Double> = emptyList()
)
