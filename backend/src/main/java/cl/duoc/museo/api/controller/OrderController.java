package cl.duoc.museo.api.controller;

import cl.duoc.museo.api.dto.Dtos.OrderItemRequest;
import cl.duoc.museo.api.dto.Dtos.OrderRequest;
import cl.duoc.museo.api.dto.Dtos.ShippingQuote;
import cl.duoc.museo.api.dto.Dtos.ShippingQuoteRequest;
import cl.duoc.museo.api.dto.Dtos.StatusUpdate;
import cl.duoc.museo.api.model.Product;
import cl.duoc.museo.api.model.PurchaseOrder;
import cl.duoc.museo.api.repository.OrderRepository;
import cl.duoc.museo.api.repository.ProductRepository;
import cl.duoc.museo.api.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Year;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class OrderController {

    private static final Set<String> STATUSES = Set.of("PENDIENTE", "CONFIRMADO", "ENVIADO", "ENTREGADO");

    private final OrderRepository orders;
    private final ProductRepository products;
    private final ShippingService shipping;

    public OrderController(OrderRepository orders, ProductRepository products, ShippingService shipping) {
        this.orders = orders;
        this.products = products;
        this.shipping = shipping;
    }

    /** Crea un pedido: valida stock, calcula envío con reglas ficticias y descuenta inventario. */
    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PurchaseOrder create(@Valid @RequestBody OrderRequest req) {
        PurchaseOrder order = new PurchaseOrder();
        order.setUserEmail(req.userEmail());
        order.setUserName(req.userName());
        order.setCountry(req.country());
        order.setShippingType(req.shippingType());
        order.setKind(req.kind() == null ? "COMPRA" : req.kind());

        boolean infoRequest = "INFO_CURSO".equals(order.getKind());
        double subtotal = 0;
        int physical = 0;
        List<OrderItemRequest> items = req.items() == null ? List.of() : req.items();
        for (OrderItemRequest it : items) {
            Product p = products.findById(it.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto " + it.productId() + " no existe"));
            if (!infoRequest) {
                if (!p.isActive() || p.getStock() < it.quantity())
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock suficiente para " + p.getName());
                p.setStock(p.getStock() - it.quantity());
                products.save(p);
                subtotal += p.getPrice() * it.quantity();
                if (!p.isCourse()) physical += it.quantity();
            }
            order.getItems().add(new PurchaseOrder.Item(p.getId(), p.getName(), it.quantity(), p.getPrice()));
        }
        double ship = infoRequest ? 0 : shipping.quote(req.country(), req.shippingType(), physical);
        if (ship < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidad de envío no disponible para " + req.country());

        order.setSubtotal(subtotal);
        order.setShipping(ship);
        order.setTotal(subtotal + ship);
        PurchaseOrder saved = orders.save(order);
        saved.setCode(String.format("#%d-%05d", Year.now().getValue(), 120 + saved.getId()));
        return orders.save(saved);
    }

    /** Historial: del cliente (userEmail) o todos (Administrador). */
    @GetMapping("/orders")
    public List<PurchaseOrder> list(@RequestParam(required = false) String userEmail) {
        return userEmail == null ? orders.findAllByOrderByCreatedAtDesc()
                : orders.findByUserEmailIgnoreCaseOrderByCreatedAtDesc(userEmail);
    }

    @PatchMapping("/orders/{id}/status")
    public PurchaseOrder updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate body) {
        if (!STATUSES.contains(body.status()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido. Use: " + STATUSES);
        PurchaseOrder o = orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        o.setStatus(body.status());
        return orders.save(o);
    }

    /** Cotización de envío sin crear pedido. */
    @PostMapping("/shipping/quote")
    public ShippingQuote quote(@Valid @RequestBody ShippingQuoteRequest req) {
        double subtotal = 0;
        int physical = 0;
        for (OrderItemRequest it : req.items() == null ? List.<OrderItemRequest>of() : req.items()) {
            Product p = products.findById(it.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Producto no existe"));
            subtotal += p.getPrice() * it.quantity();
            if (!p.isCourse()) physical += it.quantity();
        }
        double ship = shipping.quote(req.country(), req.shippingType(), physical);
        if (ship < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modalidad no disponible para ese destino");
        return new ShippingQuote(subtotal, ship, subtotal + ship, shipping.estimatedDays(req.shippingType()));
    }
}
