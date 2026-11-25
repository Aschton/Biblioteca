package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.CatalogRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class CatalogViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<CatalogRepository>()
    private val session = mockk<SessionManager>()
    private val products = listOf(
        Fixtures.product(id = 1, category = "CURSO", stock = 0).copy(name = "Curso de Curaduría"),
        Fixtures.product(id = 2, category = "LIBRO").copy(name = "Historia del Arte"),
        Fixtures.product(id = 3, category = "LIBRO").copy(description = "Libro sobre curaduría digital")
    )

    @Test fun `filtra por texto, categoria y disponibilidad`() = runTest {
        every { session.currentUser } returns flowOf(Fixtures.user(role = Role.CLIENTE))
        every { repo.observeProducts(false) } returns flowOf(products)
        coEvery { repo.refresh() } returns true

        val vm = CatalogViewModel(repo, session)
        keepCollecting(vm.state)
        assertEquals(3, vm.state.value.products.size)
        assertEquals(true, vm.state.value.online)
        assertFalse(vm.state.value.refreshing)

        vm.setQuery("curaduría")
        assertEquals(listOf(1L, 3L), vm.state.value.products.map { it.id })
        vm.toggleAvailable()
        assertEquals(listOf(3L), vm.state.value.products.map { it.id })
        vm.toggleAvailable()
        vm.setQuery("")
        vm.setCategory("LIBRO")
        assertEquals(listOf(2L, 3L), vm.state.value.products.map { it.id })
    }

    @Test fun `el administrador ve tambien los inactivos`() = runTest {
        every { session.currentUser } returns flowOf(Fixtures.user(role = Role.ADMIN))
        every { repo.observeProducts(true) } returns flowOf(products)
        coEvery { repo.refresh() } returns false

        val vm = CatalogViewModel(repo, session)
        keepCollecting(vm.state)

        assertEquals(3, vm.state.value.products.size)
        assertEquals(false, vm.state.value.online)
    }
}
