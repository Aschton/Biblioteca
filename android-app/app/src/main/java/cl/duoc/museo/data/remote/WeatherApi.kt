package cl.duoc.museo.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API EXTERNA: Open-Meteo (https://open-meteo.com) — gratuita y sin API key.
 * Entrega el clima actual y el pronóstico según latitud/longitud. La usamos para mostrar
 * el clima en la ciudad de cada museo. Sus datos NO se guardan en Room ni se envían a
 * nuestros microservicios: solo se muestran en pantalla.
 *
 * Ejemplo: https://api.open-meteo.com/v1/forecast?latitude=-33.43&longitude=-70.64
 *          &current=temperature_2m,weather_code&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=auto
 */
interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") days: Int = 3
    ): ForecastDto

    companion object {
        const val CURRENT_FIELDS = "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m"
        const val DAILY_FIELDS = "weather_code,temperature_2m_max,temperature_2m_min"
    }
}
