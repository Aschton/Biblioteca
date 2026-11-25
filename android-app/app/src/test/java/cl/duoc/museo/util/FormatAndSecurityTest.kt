package cl.duoc.museo.util

import cl.duoc.museo.data.repository.OrderStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatAndSecurityTest {
    @Test fun `sha256 conocido`() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Security.sha256("abc"))
    }

    @Test fun `formato de dinero`() {
        assertEquals("US$ 14", Format.money(14.0))
        assertEquals("US$ 1.50", Format.money2(1.5))
    }

    @Test fun `tiempo relativo`() {
        val now = System.currentTimeMillis()
        assertEquals("ahora", Format.relative(now))
        assertEquals("2 min", Format.relative(now - 2 * 60_000L - 500))
        assertEquals("2 h", Format.relative(now - 2 * 3_600_000L - 500))
        assertEquals("3 d", Format.relative(now - 3 * 86_400_000L - 500))
    }

    @Test fun `fecha legible`() {
        assertTrue(Format.date(1_700_000_000_000L).contains("2023"))
    }

    @Test fun `etiqueta de estado de pedido`() {
        assertEquals("Pendiente", OrderStatus.label(OrderStatus.PENDIENTE))
        assertEquals(4, OrderStatus.all.size)
    }
}
