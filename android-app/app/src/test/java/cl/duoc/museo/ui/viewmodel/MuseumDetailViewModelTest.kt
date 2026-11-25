package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.museo.data.local.MuseumEntity
import cl.duoc.museo.data.repository.DayForecast
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.WeatherInfo
import cl.duoc.museo.data.repository.WeatherRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class MuseumDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<MuseumRepository>(relaxUnitFun = true)
    private val weatherRepo = mockk<WeatherRepository>()
    private val museum = MutableStateFlow<MuseumEntity?>(Fixtures.museum(id = 1))
    private val favorite = MutableStateFlow(false)
    private val info = WeatherInfo(
        temperature = 22, apparentTemperature = 21, humidity = 40, windSpeed = 10,
        description = "Despejado", emoji = "☀️", isDay = true,
        days = listOf(DayForecast("2026-11-24", "Hoy", "☀️", 28, 12))
    )

    private fun vm(): MuseumDetailViewModel {
        every { repo.observeMuseum(1) } returns museum
        every { repo.observeIsFavorite(1) } returns favorite
        coEvery { repo.getMuseum(1) } answers { museum.value }
        return MuseumDetailViewModel(SavedStateHandle(mapOf("id" to 1L)), repo, weatherRepo)
    }

    @Test fun `al abrir registra la visita y consulta el clima de Open-Meteo`() = runTest {
        coEvery { weatherRepo.getWeather(-33.43, -70.64) } returns Result.success(info)

        val vm = vm()

        coVerify { repo.recordView(1) }
        assertEquals(WeatherUiState.Success(info), vm.weather.value)
    }

    @Test fun `si falla el clima se puede reintentar`() = runTest {
        coEvery { weatherRepo.getWeather(any(), any()) } returns Result.failure(IOException())
        val vm = vm()
        assertTrue(vm.weather.value is WeatherUiState.Error)

        coEvery { weatherRepo.getWeather(any(), any()) } returns Result.success(info)
        vm.retryWeather()
        assertTrue(vm.weather.value is WeatherUiState.Success)
        coVerify(exactly = 2) { weatherRepo.getWeather(-33.43, -70.64) }
    }

    @Test fun `favoritos y mensajes`() = runTest {
        coEvery { weatherRepo.getWeather(any(), any()) } returns Result.success(info)
        val vm = vm()
        keepCollecting(vm.isFavorite)

        vm.toggleFavorite()
        assertEquals("Guardado en favoritos", vm.message.value)
        coVerify { repo.setFavorite(1, true) }

        favorite.value = true
        vm.toggleFavorite()
        assertEquals("Quitado de favoritos", vm.message.value)
        vm.clearMessage()
        assertNull(vm.message.value)
    }

    @Test fun `publicar o retirar la ficha`() = runTest {
        coEvery { weatherRepo.getWeather(any(), any()) } returns Result.success(info)
        coEvery { repo.setPublished(any(), true) } returns true
        coEvery { repo.setPublished(any(), false) } returns false
        val vm = vm()
        keepCollecting(vm.museum)

        vm.setPublished(true)
        assertEquals("Ficha publicada", vm.message.value)
        vm.setPublished(false)
        assertEquals("Publicación retirada (guardado localmente)", vm.message.value)
    }

    @Test fun `publicar sin ficha cargada no hace nada`() = runTest {
        coEvery { weatherRepo.getWeather(any(), any()) } returns Result.success(info)
        val vm = vm()

        vm.setPublished(true)

        coVerify(exactly = 0) { repo.setPublished(any(), any()) }
    }

    @Test fun `eliminar ficha`() = runTest {
        coEvery { weatherRepo.getWeather(any(), any()) } returns Result.success(info)
        coEvery { repo.delete(any()) } returns false
        val vm = vm()

        vm.delete()
        assertFalse(vm.deleted.value)
        assertEquals("No se pudo eliminar: el servidor no responde", vm.message.value)

        coEvery { repo.delete(any()) } returns true
        vm.delete()
        assertTrue(vm.deleted.value)
    }
}
