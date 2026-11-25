package cl.duoc.museo.catalogo.config;

import cl.duoc.museo.catalogo.model.Product;
import cl.duoc.museo.catalogo.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Datos de demostración FICTICIOS. Solo se cargan si la tabla está vacía.
 * El orden de inserción genera los mismos IDs (1..N) que la caché inicial de la app.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository products;

    public DataLoader(ProductRepository products) { this.products = products; }

    @Override
    public void run(String... args) {
        if (products.count() > 0) return;

        List<Product> catalog = List.of(
                new Product("Curso de Curaduría Virtual", "CURSO",
                        "Aprende a diseñar exposiciones digitales: guion curatorial, montaje virtual y mediación en línea. 8 semanas.", 49, 25, 4.7),
                new Product("Historia del Arte Iberoamericano", "LIBRO",
                        "Un recorrido completo por el desarrollo del arte en Iberoamérica, con análisis de obras, contextos históricos y perspectivas contemporáneas.", 35, 40, 4.7),
                new Product("Kit de Conservación de Documentos", "MATERIAL",
                        "Carpetas libres de ácido, guantes de nitrilo, pinceles y fichas de registro para conservación preventiva.", 120, 12, 4.5),
                new Product("Curso Museos y Comunidad", "CURSO",
                        "Estrategias de desarrollo de audiencias y vinculación territorial para museos. 6 semanas.", 39, 30, 4.6),
                new Product("Curso de Museología Digital", "CURSO",
                        "Digitalización de colecciones, metadatos y plataformas de museos virtuales. 10 semanas.", 59, 20, 4.8),
                new Product("El Arte Colonial en Iberoamérica", "LIBRO",
                        "Libro digital sobre la historia del arte colonial en la región iberoamericana.", 28, 100, 4.6),
                new Product("Guantes de algodón para manipulación", "MATERIAL",
                        "Pack de 6 pares de guantes de algodón para manipulación de obras y documentos.", 15, 60, 4.3),
                new Product("Réplica de cerámica precolombina", "ARTICULO",
                        "Réplica artesanal con certificado de origen, ideal para material educativo.", 45, 8, 4.4)
        );
        catalog.forEach(p -> {
            Product saved = products.save(p);
            saved.setQrCode("PROD:" + saved.getId());
            products.save(saved);
        });
    }
}
