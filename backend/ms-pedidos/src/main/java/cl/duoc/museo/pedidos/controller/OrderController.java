package cl.duoc.museo.pedidos.controller;

import cl.duoc.museo.pedidos.dto.OrderRequest;
import cl.duoc.museo.pedidos.dto.ShippingQuoteResponse;
import cl.duoc.museo.pedidos.dto.ShippingQuoteRequest;
import cl.duoc.museo.pedidos.dto.StatusUpdateRequest;
import cl.duoc.museo.pedidos.model.PurchaseOrder;
import cl.duoc.museo.pedidos.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRUD de pedidos/solicitudes y cotización de envío. */
@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrder create(@Valid @RequestBody OrderRequest req) { return service.create(req); }

    /** Historial: del cliente (userEmail) o todos (Administrador). */
    @GetMapping("/orders")
    public List<PurchaseOrder> list(@RequestParam(required = false) String userEmail) { return service.list(userEmail); }

    @GetMapping("/orders/{id}")
    public PurchaseOrder get(@PathVariable Long id) { return service.get(id); }

    @PatchMapping("/orders/{id}/status")
    public PurchaseOrder updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest body) {
        return service.updateStatus(id, body.status());
    }

    @DeleteMapping("/orders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @PostMapping("/shipping/quote")
    public ShippingQuoteResponse quote(@Valid @RequestBody ShippingQuoteRequest req) { return service.quote(req); }
}
