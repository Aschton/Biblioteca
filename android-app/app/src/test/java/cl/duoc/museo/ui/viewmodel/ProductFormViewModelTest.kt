package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.data.repository.NotificationRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProductFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<CatalogRepository>()
    private val notifications = mockk<NotificationRepository>(relaxUnitFun = true)

    @Test fun `valida los campos`() = runTest {
        val vm = ProductFormViewModel(SavedStateHandle(), repo, notifications)
        vm.update { it.copy(name = "ab", description = "corta", price = "-1", stock = "x") }

        vm.save()

        assertEquals(setOf("name", "description", "price", "stock"), vm.form.value.errors.keys)
        coVerify(exactly = 0) { repo.save(any(), any()) }
    }

    @Test fun `nuevo curso avisa a los clientes`() = runTest {
        coEvery { repo.save(any(), true) } returns true
        val vm = ProductFormViewModel(SavedStateHandle(), repo, notifications)
        vm.update { it.copy(name = "Curso nuevo", category = "CURSO", description = "Descripción del curso", price = "49,5", stock = "20") }

        vm.save()

        coVerify { repo.save(match { it.price == 49.5 && it.stock == 20 }, true) }
        coVerify { notifications.publish("Nuevo curso disponible", "Curso nuevo", "CURSO", Role.CLIENTE.name, any(), "catalog", any()) }
        assertTrue(vm.form.value.saved)
    }

    @Test fun `nuevo libro avisa como publicacion y uno inactivo no avisa`() = runTest {
        coEvery { repo.save(any(), true) } returns true
        val vm = ProductFormViewModel(SavedStateHandle(), repo, notifications)
        vm.update { it.copy(name = "Libro nuevo", category = "LIBRO", description = "Descripción del libro", price = "10", stock = "1") }
        vm.save()
        coVerify { notifications.publish("Nuevo en el catálogo", any(), "PUBLICACION", any(), any(), any(), any()) }

        val vm2 = ProductFormViewModel(SavedStateHandle(), repo, notifications)
        vm2.update { it.copy(name = "Oculto", description = "Descripción del producto", price = "10", stock = "1", active = false) }
        vm2.save()
        coVerify(exactly = 1) { notifications.publish(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test fun `editar, desactivar y eliminar`() = runTest {
        val p = Fixtures.product(id = 2)
        coEvery { repo.getProduct(2) } returns p
        coEvery { repo.save(any(), false) } returns true
        coEvery { repo.setActive(p, false) } returns true
        val vm = ProductFormViewModel(SavedStateHandle(mapOf("id" to 2L)), repo, notifications)

        assertFalse(vm.isNew)
        assertEquals("Producto 2", vm.form.value.name)
        vm.save()
        coVerify { repo.save(match { it.id == 2L }, false) }

        vm.deactivate()
        assertFalse(vm.form.value.active)

        coEvery { repo.delete(p) } returns false
        vm.delete()
        assertEquals("No se pudo eliminar: el servidor no responde", vm.form.value.error)
        vm.clearError()
        assertNull(vm.form.value.error)

        coEvery { repo.delete(p) } returns true
        vm.delete()
        assertTrue(vm.form.value.deleted)
    }

    @Test fun `producto nuevo no se puede desactivar ni eliminar`() = runTest {
        val vm = ProductFormViewModel(SavedStateHandle(), repo, notifications)

        vm.deactivate()
        vm.delete()

        coVerify(exactly = 0) { repo.setActive(any(), any()) }
        coVerify(exactly = 0) { repo.delete(any()) }
    }
}
