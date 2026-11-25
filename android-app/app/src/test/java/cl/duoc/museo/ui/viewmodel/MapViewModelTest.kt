package cl.duoc.museo.ui.viewmodel

import cl.duoc.museo.data.SessionManager
import cl.duoc.museo.data.local.Role
import cl.duoc.museo.data.repository.MuseumRepository
import cl.duoc.museo.testutil.Fixtures
import cl.duoc.museo.testutil.MainDispatcherRule
import cl.duoc.museo.testutil.keepCollecting
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<MuseumRepository>()
    private val session = mockk<SessionManager>()
    private val museums = listOf(
        Fixtures.museum(id = 1, country = "Chile", project = "Red Iberoamericana"),
        Fixtures.museum(id = 2, country = "Perú", project = "Arte Moderno en Línea"),
        Fixtures.museum(id = 3, country = "Chile", project = "Arte Moderno en Línea")
    )

    @Test fun `cliente ve solo publicados y puede filtrar por pais y proyecto`() = runTest {
        every { session.currentUser } returns flowOf(Fixtures.user(role = Role.CLIENTE))
        every { repo.observeMuseums(false) } returns flowOf(museums)
        coEvery { repo.refresh() } returns false

        val vm = MapViewModel(repo, session)
        keepCollecting(vm.state)
        assertEquals(3, vm.state.value.museums.size)
        assertEquals(false, vm.state.value.online)

        vm.setFilter(MapFilter.PAIS)
        assertEquals(listOf("Chile", "Perú"), vm.state.value.options)
        vm.setFilterValue("Chile")
        assertEquals(listOf(1L, 3L), vm.state.value.museums.map { it.id })

        vm.setFilter(MapFilter.PROYECTO)
        assertEquals(listOf("Arte Moderno en Línea", "Red Iberoamericana"), vm.state.value.options)
        assertNull(vm.state.value.filterValue)
        vm.setFilterValue("Arte Moderno en Línea")
        assertEquals(listOf(2L, 3L), vm.state.value.museums.map { it.id })

        vm.select(museums[0])
        assertEquals(1L, vm.state.value.selected?.id)
        vm.setFilter(MapFilter.TODOS)
        assertTrue(vm.state.value.options.isEmpty())
    }

    @Test fun `administrador tambien ve fichas no publicadas`() = runTest {
        every { session.currentUser } returns flowOf(Fixtures.user(role = Role.ADMIN))
        every { repo.observeMuseums(true) } returns flowOf(museums)
        coEvery { repo.refresh() } returns true

        val vm = MapViewModel(repo, session)
        keepCollecting(vm.state)

        assertEquals(3, vm.state.value.museums.size)
        assertEquals(true, vm.state.value.online)
    }
}
