package cl.duoc.museo.pedidos;

import cl.duoc.museo.pedidos.service.ShippingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShippingServiceTest {

    private final ShippingService service = new ShippingService();

    @Test
    void reglasDeEnvioFicticias() {
        assertEquals(4.0, service.quote("Chile", "ESTANDAR", 1));
        assertEquals(6.0, service.quote("Chile", "ESTANDAR", 3));
        assertEquals(12.0 * 1.6, service.quote("Perú", "EXPRESS", 1), 0.001);
        assertEquals(20.0, service.quote("España", "ESTANDAR", 1));
        assertEquals(0.0, service.quote("Chile", "RETIRO", 2));
        assertEquals(-1.0, service.quote("Perú", "RETIRO", 2));
        assertEquals(0.0, service.quote("Perú", "EXPRESS", 0));
    }

    @Test
    void diasEstimados() {
        assertEquals("2-5 días", service.estimatedDays("EXPRESS"));
        assertEquals("7-14 días", service.estimatedDays("ESTANDAR"));
    }
}
