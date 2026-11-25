package cl.duoc.museo.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import cl.duoc.museo.data.repository.QrRepository
import cl.duoc.museo.data.repository.QrResult
import cl.duoc.museo.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class QrResultViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = mockk<QrRepository>()

    @Test fun `resuelve el codigo recibido`() = runTest {
        val r = QrResult("MUSEO:1", "MUSEUM", 1, "Museo 1", "desc", "Museo virtual")
        coEvery { repo.resolve("MUSEO:1") } returns Result.success(r)

        val vm = QrResultViewModel(SavedStateHandle(mapOf("code" to "MUSEO:1")), repo)

        assertFalse(vm.state.value.loading)
        assertEquals(r, vm.state.value.result)
    }

    @Test fun `muestra el error si el codigo no existe`() = runTest {
        coEvery { repo.resolve("") } returns Result.failure(IllegalArgumentException("Código desconocido"))

        val vm = QrResultViewModel(SavedStateHandle(), repo)

        assertNull(vm.state.value.result)
        assertEquals("Código desconocido", vm.state.value.error)
    }
}
