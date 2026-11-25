package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MuseumFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<MuseumRepository>()
    private val notifications = mockk<NotificationRepository>(relaxUnitFun = true)

    @Test fun `formulario vacio muestra todos los errores`() = runTest {
        val vm = MuseumFormViewModel(SavedStateHandle(), repo, notifications)
        assertTrue(vm.isNew)

        vm.save(publishDirectly = true)

        assertEquals(setOf("name", "city", "latitude", "longitude", "description", "virtualTourUrl"), vm.form.value.errors.keys)
        coVerify(exactly = 0) { repo.save(any(), any()) }
    }

    @Test fun `coordenadas fuera de Iberoamerica no se aceptan`() = runTest {
        val vm = MuseumFormViewModel(SavedStateHandle(), repo, notifications)
        vm.update { it.copy(latitude = "70", longitude = "50,5") }

        vm.save(publishDirectly = false)

        assertTrue("latitude" in vm.form.value.errors)
        assertTrue("longitude" in vm.form.value.errors)
    }

    @Test fun `ficha nueva valida se guarda y avisa al administrador`() = runTest {
        coEvery { repo.save(any(), true) } returns true
        val vm = MuseumFormViewModel(SavedStateHandle(), repo, notifications)
        vm.update {
            it.copy(
                name = " Museo Nuevo ", city = "Valparaíso", latitude = "-33,04", longitude = "-71.62",
                description = "Una descripción suficientemente larga", virtualTourUrl = "https://tour.cl/nuevo"
            )
        }

        vm.save(publishDirectly = true)

        coVerify { repo.save(match { it.name == "Museo Nuevo" && it.latitude == -33.04 && it.published }, true) }
        coVerify { notifications.publish("Nueva ficha registrada", any(), "MUSEO", Role.ADMIN.name, any(), "map", any()) }
        assertTrue(vm.form.value.saved)
        assertTrue(vm.form.value.savedOnline)
        assertFalse(vm.form.value.saving)
    }

    @Test fun `editar carga la ficha y mantiene su estado de publicacion`() = runTest {
        coEvery { repo.getMuseum(4) } returns Fixtures.museum(id = 4, published = false)
        coEvery { repo.save(any(), false) } returns false
        val vm = MuseumFormViewModel(SavedStateHandle(mapOf("id" to 4L)), repo, notifications)

        assertFalse(vm.isNew)
        assertEquals("Museo 4", vm.form.value.name)

        vm.save(publishDirectly = true)

        coVerify { repo.save(match { it.id == 4L && !it.published }, false) }
        coVerify { notifications.publish("Ficha actualizada", any(), any(), any(), any(), any(), any()) }
        assertFalse(vm.form.value.savedOnline)
    }
}
