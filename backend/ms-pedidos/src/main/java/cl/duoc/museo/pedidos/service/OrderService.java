package cl.duoc.museo.pedidos.service;

import cl.duoc.museo.pedidos.client.CatalogClient;
import cl.duoc.museo.pedidos.dto.OrderItemRequest;
import cl.duoc.museo.pedidos.dto.OrderRequest;
import cl.duoc.museo.pedidos.dto.ProductDto;
import cl.duoc.museo.pedidos.dto.ShippingQuoteResponse;
import cl.duoc.museo.pedidos.dto.ShippingQuoteRequest;
import cl.duoc.museo.pedidos.model.PurchaseOrder;
import cl.duoc.museo.pedidos.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Lógica de negocio de pedidos. Consulta precios y stock en ms-catalogo. */
@Service
public class OrderService {

    public static final Set<String> STATUSES = Set.of("PENDIENTE", "CONFIRMADO", "ENVIADO", "ENTREGADO");

    private final OrderRepository orders;
    private final CatalogClient catalog;
    private final ShippingService shipping;

    public OrderService(OrderRepository orders, CatalogClient catalog, ShippingService shipping) {
        this.orders = orders;
        this.catalog = catalog;
        this.shipping = shipping;
    }

    /** Crea un pedido: valida stock, calcula envío con reglas ficticias y descuenta inventario en ms-catalogo. */
    public PurchaseOrder create(OrderRequest req) {
        PurchaseOrder order = new PurchaseOrder();
        order.setUserEmail(req.userEmail());
        order.setUserName(req.userName());
        order.setCountry(req.country());
        order.setShippingType(req.shippingType());
        order.setKind(req.kind() == null ? "COMPRA" : req.kind());

        boolean infoRequest = "INFO_CURSO".equals(order.getKind());
        List<OrderItemRequest> items = req.items() == null ? List.of() : req.items();

        // 1) Validar todo antes de modificar nada
        List<ProductDto> found = new ArrayList<>();
        double subtotal = 0;
        int physical = 0;
        for (OrderItemRequest it : items) {
            ProductDto p = catalog.getProduct(it.productId());
            if (!infoRequest) {
                if (!p.active() || p.stock() < it.quantity())
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock suficiente para " + p.name());
                subtotal += p.price() * it.quantity();
                if (!p.isCourse()) physical += it.quantity();
            }
            found.add(p);
        }
        double ship = infoRequest ? 0 : shipping.quote(req.country(), req.shippingType(), physical);
        if (ship < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidad de envío no disponible para " + req.country());

        // 2) Descontar inventario en ms-catalogo
        if (!infoRequest) {
            for (OrderItemRequest it : items) catalog.decreaseStock(it.productId(), it.quantity());
        }

        for (int i = 0; i < items.size(); i++) {
            ProductDto p = found.get(i);
            order.getItems().add(new PurchaseOrder.Item(p.id(), p.name(), items.get(i).quantity(), p.price()));
        }
        order.setSubtotal(subtotal);
        order.setShipping(ship);
        order.setTotal(subtotal + ship);
        PurchaseOrder saved = orders.save(order);
        saved.setCode(String.format("#%d-%05d", Year.now().getValue(), 120 + saved.getId()));
        return orders.save(saved);
    }

    public List<PurchaseOrder> list(String userEmail) {
        return userEmail == null ? orders.findAllByOrderByCreatedAtDesc()
                : orders.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
    }

    public PurchaseOrder get(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
    }

    public PurchaseOrder updateStatus(Long id, String status) {
        if (!STATUSES.contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido. Use: " + STATUSES);
        PurchaseOrder o = get(id);
        o.setStatus(status);
        return orders.save(o);
    }

    public void delete(Long id) {
        if (!orders.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado");
        orders.deleteById(id);
    }

    /** Cotización de envío sin crear pedido. */
    public ShippingQuoteResponse quote(ShippingQuoteRequest req) {
        double subtotal = 0;
        int physical = 0;
        for (OrderItemRequest it : req.items() == null ? List.<OrderItemRequest>of() : req.items()) {
            ProductDto p = catalog.getProduct(it.productId());
            subtotal += p.price() * it.quantity();
            if (!p.isCourse()) physical += it.quantity();
        }
        double ship = shipping.quote(req.country(), req.shippingType(), physical);
        if (ship < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidad no disponible para ese destino");
        return new ShippingQuoteResponse(subtotal, ship, subtotal + ship, shipping.estimatedDays(req.shippingType()));
    }
}
