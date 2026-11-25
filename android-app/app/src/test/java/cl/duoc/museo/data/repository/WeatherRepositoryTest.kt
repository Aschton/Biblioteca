package cl.duoc.museo.data.repository

import cl.duoc.museo.data.remote.CurrentWeatherDto
import cl.duoc.museo.data.remote.DailyWeatherDto
import cl.duoc.museo.data.remote.ForecastDto
import cl.duoc.museo.data.remote.OpenMeteoApi
import cl.duoc.museo.data.repository.WeatherRepository.Companion.toWeatherInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class WeatherRepositoryTest {

    private val api = mockk<OpenMeteoApi>()
    private val repo = WeatherRepository(api)

    private val forecast = ForecastDto(
        latitude = -33.43, longitude = -70.64,
        current = CurrentWeatherDto(temperature = 21.6, humidity = 40, apparentTemperature = 20.4, isDay = 1, weatherCode = 0, windSpeed = 9.7),
        daily = DailyWeatherDto(
            time = listOf("2026-11-24", "2026-11-25", "2026-11-26"),
            weatherCode = listOf(0, 3, 61),
            maxTemperature = listOf(28.4, 25.0, 19.6),
            minTemperature = listOf(11.5, 10.0, 9.4)
        )
    )

    @Test fun `obtiene y traduce el clima`() = runTest {
        coEvery { api.forecast(-33.43, -70.64, any(), any(), any(), any()) } returns forecast

        val info = repo.getWeather(-33.43, -70.64).getOrThrow()

        assertEquals(22, info.temperature)
        assertEquals(20, info.apparentTemperature)
        assertEquals(40, info.humidity)
        assertEquals(10, info.windSpeed)
        assertEquals("Despejado", info.description)
        assertEquals("☀️", info.emoji)
        assertTrue(info.isDay)
        assertEquals(3, info.days.size)
        assertEquals("Hoy", info.days[0].label)
        assertEquals("Mañana", info.days[1].label)
        assertEquals("Jue", info.days[2].label) // 26-11-2026 es jueves
        assertEquals(28, info.days[0].max)
        assertEquals("🌧️", info.days[2].emoji)
        coVerify(exactly = 1) { api.forecast(-33.43, -70.64, any(), any(), any(), any()) }
    }

    @Test fun `si no hay internet devuelve failure`() = runTest {
        coEvery { api.forecast(any(), any(), any(), any(), any(), any()) } throws IOException("sin red")

        val r = repo.getWeather(0.0, 0.0)

        assertTrue(r.isFailure)
    }

    @Test fun `sin clima actual es un error y sin pronostico diario no hay dias`() {
        assertTrue(runCatching { forecast.copy(current = null).toWeatherInfo() }.isFailure)
        assertTrue(forecast.copy(daily = null).toWeatherInfo().days.isEmpty())
        // Listas incompletas: se descartan los días sin datos
        val incompleto = forecast.copy(daily = forecast.daily!!.copy(minTemperature = listOf(11.5)))
        assertEquals(1, incompleto.toWeatherInfo().days.size)
    }

    @Test fun `de noche cambia el icono`() {
        val noche = forecast.copy(current = forecast.current!!.copy(isDay = 0)).toWeatherInfo()
        assertFalse(noche.isDay)
        assertEquals("🌙", noche.emoji)
    }

    @Test fun `todos los codigos WMO tienen descripcion e icono`() {
        val esperado = mapOf(
            0 to "Despejado", 1 to "Mayormente despejado", 2 to "Parcialmente nublado", 3 to "Nublado",
            45 to "Niebla", 48 to "Niebla", 51 to "Llovizna", 53 to "Llovizna", 55 to "Llovizna",
            56 to "Llovizna helada", 57 to "Llovizna helada", 61 to "Lluvia débil", 63 to "Lluvia moderada",
            65 to "Lluvia intensa", 66 to "Lluvia helada", 67 to "Lluvia helada", 71 to "Nieve", 73 to "Nieve",
            75 to "Nieve", 77 to "Nieve", 80 to "Chubascos", 81 to "Chubascos", 82 to "Chubascos",
            85 to "Chubascos de nieve", 86 to "Chubascos de nieve", 95 to "Tormenta eléctrica",
            96 to "Tormenta con granizo", 99 to "Tormenta con granizo", 1234 to "Sin datos"
        )
        esperado.forEach { (code, text) -> assertEquals("código $code", text, WeatherCodes.description(code)) }

        assertEquals("🌤️", WeatherCodes.emoji(1, true))
        assertEquals("☁️", WeatherCodes.emoji(2, false))
        assertEquals("☁️", WeatherCodes.emoji(3))
        assertEquals("🌫️", WeatherCodes.emoji(45))
        assertEquals("❄️", WeatherCodes.emoji(71))
        assertEquals("⛈️", WeatherCodes.emoji(95))
        assertEquals("🌡️", WeatherCodes.emoji(1234))
    }

    @Test fun `etiqueta de dia con fecha invalida devuelve el texto original`() {
        assertEquals("no-es-fecha", WeatherCodes.dayLabel("no-es-fecha", 3))
        assertEquals("Lun", WeatherCodes.dayLabel("2026-11-23", 4))
    }
}
