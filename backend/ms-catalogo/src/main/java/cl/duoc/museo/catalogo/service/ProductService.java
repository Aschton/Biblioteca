package cl.duoc.museo.catalogo.service;

import cl.duoc.museo.catalogo.dto.AvailabilityDto;
import cl.duoc.museo.catalogo.model.Product;
import cl.duoc.museo.catalogo.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Lógica de negocio del catálogo: CRUD, disponibilidad y descuento de stock. */
@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) { this.repo = repo; }

    /** Catálogo con filtros opcionales por categoría y texto. */
    public List<Product> list(String category, String q) {
        return repo.findAll().stream()
                .filter(p -> category == null || p.getCategory().equalsIgnoreCase(category))
                .filter(p -> q == null || p.getName().toLowerCase().contains(q.toLowerCase()))
                .toList();
    }

    public Product get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    /** Crea el producto y le asigna su código QR "PROD:<id>". */
    public Product create(Product product) {
        product.setId(null);
        Product saved = repo.save(product);
        saved.setQrCode("PROD:" + saved.getId());
        return repo.save(saved);
    }

    public Product update(Long id, Product body) {
        get(id);
        body.setId(id);
        body.setQrCode("PROD:" + id);
        return repo.save(body);
    }

    public Product deactivate(Long id) {
        Product p = get(id);
        p.setActive(false);
        return repo.save(p);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        repo.deleteById(id);
    }

    public AvailabilityDto availability(Long id, int qty) {
        Product p = get(id);
        return new AvailabilityDto(id, p.isActive() && p.getStock() >= qty, p.getStock());
    }

    /** Descuenta inventario (lo usa ms-pedidos). Lanza 409 si no alcanza el stock. */
    @Transactional
    public Product decreaseStock(Long id, int quantity) {
        Product p = get(id);
        if (!p.isActive() || p.getStock() < quantity)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock suficiente para " + p.getName());
        p.setStock(p.getStock() - quantity);
        return repo.save(p);
    }
}
