package cl.duoc.museo.pedidos;

import cl.duoc.museo.pedidos.client.CatalogClient;
import cl.duoc.museo.pedidos.dto.OrderItemRequest;
import cl.duoc.museo.pedidos.dto.OrderRequest;
import cl.duoc.museo.pedidos.dto.ProductDto;
import cl.duoc.museo.pedidos.model.PurchaseOrder;
import cl.duoc.museo.pedidos.repository.OrderRepository;
import cl.duoc.museo.pedidos.service.OrderService;
import cl.duoc.museo.pedidos.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba unitaria con Mockito: ms-catalogo se simula, no hace falta tenerlo encendido. */
class OrderServiceTest {

    private OrderRepository repo;
    private CatalogClient catalog;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repo = mock(OrderRepository.class);
        catalog = mock(CatalogClient.class);
        service = new OrderService(repo, catalog, new ShippingService());
        // Simula la base de datos: devuelve el mismo pedido asignándole id 1
        when(repo.save(any(PurchaseOrder.class))).thenAnswer(inv -> {
            PurchaseOrder o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", 1L);
            return o;
        });
    }

    @Test
    void creaPedidoCalculaTotalYDescuentaStock() {
        when(catalog.getProduct(1L)).thenReturn(new ProductDto(1L, "Libro", "LIBRO", 35.0, 10, true));
        OrderRequest req = new OrderRequest("cliente@museo.cl", "Cliente", List.of(new OrderItemRequest(1L, 2)), "Chile", "ESTANDAR", null);

        PurchaseOrder o = service.create(req);

        assertEquals(70.0, o.getSubtotal());
        assertEquals(5.0, o.getShipping());
        assertEquals(75.0, o.getTotal());
        assertEquals("COMPRA", o.getKind());
        verify(catalog).decreaseStock(1L, 2);
    }

    @Test
    void rechazaSiNoHayStock() {
        when(catalog.getProduct(1L)).thenReturn(new ProductDto(1L, "Libro", "LIBRO", 35.0, 1, true));
        OrderRequest req = new OrderRequest("cliente@museo.cl", "Cliente", List.of(new OrderItemRequest(1L, 5)), "Chile", "ESTANDAR", null);

        assertThrows(ResponseStatusException.class, () -> service.create(req));
        verify(catalog, never()).decreaseStock(anyLong(), anyInt());
    }

    @Test
    void solicitudDeInformacionNoCobraNiDescuenta() {
        when(catalog.getProduct(2L)).thenReturn(new ProductDto(2L, "Curso", "CURSO", 49.0, 0, true));
        OrderRequest req = new OrderRequest("cliente@museo.cl", "Cliente", List.of(new OrderItemRequest(2L, 1)), "Chile", "DIGITAL", "INFO_CURSO");

        PurchaseOrder o = service.create(req);

        assertEquals(0.0, o.getTotal());
        verify(catalog, never()).decreaseStock(anyLong(), anyInt());
    }

    @Test
    void rechazaEstadoInvalido() {
        assertThrows(ResponseStatusException.class, () -> service.updateStatus(1L, "PERDIDO"));
    }
}
