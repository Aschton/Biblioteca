package cl.duoc.museo.catalogo.controller;

import cl.duoc.museo.catalogo.dto.AvailabilityDto;
import cl.duoc.museo.catalogo.dto.StockChangeRequest;
import cl.duoc.museo.catalogo.model.Product;
import cl.duoc.museo.catalogo.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints REST del catálogo. La lógica está en ProductService. */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String q) {
        return service.list(category, q);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) { return service.create(product); }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product body) { return service.update(id, body); }

    @PatchMapping("/{id}/deactivate")
    public Product deactivate(@PathVariable Long id) { return service.deactivate(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/{id}/availability")
    public AvailabilityDto availability(@PathVariable Long id, @RequestParam(defaultValue = "1") int qty) {
        return service.availability(id, qty);
    }

    @PostMapping("/{id}/decrease-stock")
    public Product decreaseStock(@PathVariable Long id, @Valid @RequestBody StockChangeRequest body) {
        return service.decreaseStock(id, body.quantity());
    }
}
