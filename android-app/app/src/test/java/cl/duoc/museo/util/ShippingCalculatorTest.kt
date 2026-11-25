package cl.duoc.museo.util

import cl.duoc.museo.util.ShippingCalculator.ShippingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShippingCalculatorTest {
    @Test fun `envio nacional estandar y express`() {
        assertEquals(4.0, ShippingCalculator.quote("Chile", ShippingType.ESTANDAR, 1), 0.001)
        assertEquals(6.0, ShippingCalculator.quote("Chile", ShippingType.ESTANDAR, 3), 0.001)
        assertEquals(4.0 * 1.8, ShippingCalculator.quote("Chile", ShippingType.EXPRESS, 1), 0.001)
    }

    @Test fun `envio latinoamerica y resto del mundo`() {
        assertEquals(14.0, ShippingCalculator.quote("Perú", ShippingType.ESTANDAR, 2), 0.001)
        assertEquals(12.0 * 1.6, ShippingCalculator.quote("México", ShippingType.EXPRESS, 1), 0.001)
        assertEquals(20.0, ShippingCalculator.quote("España", ShippingType.ESTANDAR, 1), 0.001)
        assertEquals(30.0, ShippingCalculator.quote("Otro", ShippingType.EXPRESS, 1), 0.001)
    }

    @Test fun `retiro solo en Chile y cursos no pagan envio`() {
        assertEquals(0.0, ShippingCalculator.quote("Chile", ShippingType.RETIRO, 2), 0.001)
        assertEquals(-1.0, ShippingCalculator.quote("Perú", ShippingType.RETIRO, 2), 0.001)
        assertEquals(0.0, ShippingCalculator.quote("España", ShippingType.EXPRESS, 0), 0.001)
    }

    @Test fun `pais nacional y listas`() {
        assertTrue(ShippingCalculator.isNational("Chile"))
        assertFalse(ShippingCalculator.isNational("Perú"))
        assertTrue("Chile" in ShippingCalculator.countries)
        assertEquals("Estándar", ShippingType.ESTANDAR.label)
        assertEquals("2-5 días", ShippingType.EXPRESS.days)
    }
}
