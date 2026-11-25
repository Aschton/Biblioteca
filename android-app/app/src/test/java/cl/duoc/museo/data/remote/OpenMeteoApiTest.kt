package cl.duoc.museo.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Prueba de la API externa con un servidor HTTP falso (MockWebServer): verifica que Retrofit
 * arma bien la URL de Open-Meteo y que Gson lee el JSON con el formato real de la API.
 */
class OpenMeteoApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: OpenMeteoApi

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `lee el clima actual y el pronostico`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(SAMPLE_JSON))

        val r = api.forecast(-33.43, -70.64)

        assertEquals(21.4, r.current!!.temperature, 0.001)
        assertEquals(35, r.current!!.humidity)
        assertEquals(2, r.current!!.weatherCode)
        assertEquals(1, r.current!!.isDay)
        assertEquals(12.6, r.current!!.windSpeed, 0.001)
        assertEquals(listOf("2026-11-24", "2026-11-25", "2026-11-26"), r.daily!!.time)
        assertEquals(28.1, r.daily!!.maxTemperature[0], 0.001)
        assertEquals("America/Santiago", r.timezone)

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.startsWith("/v1/forecast?"))
        assertTrue(path.contains("latitude=-33.43"))
        assertTrue(path.contains("longitude=-70.64"))
        assertTrue(path.contains("current="))
        assertTrue(path.contains("timezone=auto"))
    }

    companion object {
        const val SAMPLE_JSON = """
        {
          "latitude": -33.43, "longitude": -70.64, "timezone": "America/Santiago",
          "current": {
            "time": "2026-11-24T15:00", "interval": 900, "temperature_2m": 21.4,
            "relative_humidity_2m": 35, "apparent_temperature": 20.1, "is_day": 1,
            "weather_code": 2, "wind_speed_10m": 12.6
          },
          "daily": {
            "time": ["2026-11-24", "2026-11-25", "2026-11-26"],
            "weather_code": [2, 0, 61],
            "temperature_2m_max": [28.1, 30.0, 22.5],
            "temperature_2m_min": [11.2, 12.0, 10.4]
          }
        }
        """
    }
}
