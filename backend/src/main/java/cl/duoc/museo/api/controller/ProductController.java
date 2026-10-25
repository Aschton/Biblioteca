package cl.duoc.museo.api.controller;

import cl.duoc.museo.api.dto.Dtos.Availability;
import cl.duoc.museo.api.model.Product;
import cl.duoc.museo.api.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) { this.repo = repo; }

    /** Catálogo con filtros opcionales por categoría y texto. */
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String q) {
        return repo.findAll().stream()
                .filter(p -> category == null || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> q == null || p.getName().toLowerCase().contains(q.toLowerCase()))
                .toList();
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody Product product) {
        product.setId(null);
        Product saved = repo.save(product);
        saved.setQrCode("PROD:" + saved.getId());
        return repo.save(saved);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product body) {
        get(id);
        body.setId(id);
        body.setQrCode("PROD:" + id);
        return repo.save(body);
    }

    @PatchMapping("/{id}/deactivate")
    public Product deactivate(@PathVariable Long id) {
        Product p = get(id);
        p.setActive(false);
        return repo.save(p);
    }

    /** Validación de disponibilidad antes de confirmar una solicitud. */
    @GetMapping("/{id}/availability")
    public Availability availability(@PathVariable Long id, @RequestParam(defaultValue = "1") int qty) {
        Product p = get(id);
        return new Availability(id, p.isActive() && p.getStock() >= qty, p.getStock());
    }
}
