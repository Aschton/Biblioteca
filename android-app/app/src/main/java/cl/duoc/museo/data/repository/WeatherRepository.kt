package cl.duoc.museo.data.repository

import cl.duoc.museo.data.remote.ForecastDto
import cl.duoc.museo.data.remote.OpenMeteoApi
import kotlin.math.roundToInt

/** Clima ya traducido a algo que la pantalla puede mostrar directamente. */
data class WeatherInfo(
    val temperature: Int,
    val apparentTemperature: Int,
    val humidity: Int,
    val windSpeed: Int,
    val description: String,
    val emoji: String,
    val isDay: Boolean,
    val days: List<DayForecast>
)

data class DayForecast(val date: String, val label: String, val emoji: String, val max: Int, val min: Int)

/**
 * Traduce los códigos WMO que entrega Open-Meteo a texto en español.
 * Tabla oficial: https://open-meteo.com/en/docs (WMO Weather interpretation codes).
 */
object WeatherCodes {
    fun description(code: Int): String = when (code) {
        0 -> "Despejado"
        1 -> "Mayormente despejado"
        2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55 -> "Llovizna"
        56, 57 -> "Llovizna helada"
        61 -> "Lluvia débil"
        63 -> "Lluvia moderada"
        65 -> "Lluvia intensa"
        66, 67 -> "Lluvia helada"
        71, 73, 75, 77 -> "Nieve"
        80, 81, 82 -> "Chubascos"
        85, 86 -> "Chubascos de nieve"
        95 -> "Tormenta eléctrica"
        96, 99 -> "Tormenta con granizo"
        else -> "Sin datos"
    }

    fun emoji(code: Int, isDay: Boolean = true): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1, 2 -> if (isDay) "🌤️" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    private val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    /** "2026-11-24" → "Hoy", "Mañana" o el día de la semana abreviado. */
    fun dayLabel(isoDate: String, index: Int): String = when (index) {
        0 -> "Hoy"
        1 -> "Mañana"
        else -> runCatching {
            weekDays[java.time.LocalDate.parse(isoDate).dayOfWeek.value - 1]
        }.getOrDefault(isoDate)
    }
}

/**
 * Consume la API externa Open-Meteo vía Retrofit. No usa Room ni los microservicios propios:
 * el clima se pide cada vez que se abre la ficha de un museo.
 */
class WeatherRepository(private val api: OpenMeteoApi) {

    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherInfo> = runCatching {
        api.forecast(latitude, longitude).toWeatherInfo()
    }

    companion object {
        fun ForecastDto.toWeatherInfo(): WeatherInfo {
            val c = current ?: error("Open-Meteo no devolvió el clima actual")
            val day = c.isDay == 1
            val d = daily
            val days = if (d == null) emptyList() else d.time.indices.mapNotNull { i ->
                val code = d.weatherCode.getOrNull(i) ?: return@mapNotNull null
                val max = d.maxTemperature.getOrNull(i) ?: return@mapNotNull null
                val min = d.minTemperature.getOrNull(i) ?: return@mapNotNull null
                DayForecast(d.time[i], WeatherCodes.dayLabel(d.time[i], i), WeatherCodes.emoji(code), max.roundToInt(), min.roundToInt())
            }
            return WeatherInfo(
                temperature = c.temperature.roundToInt(),
                apparentTemperature = c.apparentTemperature.roundToInt(),
                humidity = c.humidity,
                windSpeed = c.windSpeed.roundToInt(),
                description = WeatherCodes.description(c.weatherCode),
                emoji = WeatherCodes.emoji(c.weatherCode, day),
                isDay = day,
                days = days
            )
        }
    }
}
