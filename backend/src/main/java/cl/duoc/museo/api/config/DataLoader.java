package cl.duoc.museo.api.config;

import cl.duoc.museo.api.model.Museum;
import cl.duoc.museo.api.model.Notice;
import cl.duoc.museo.api.model.Product;
import cl.duoc.museo.api.repository.MuseumRepository;
import cl.duoc.museo.api.repository.NoticeRepository;
import cl.duoc.museo.api.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Datos de demostración FICTICIOS. El orden de inserción genera los mismos IDs (1..N)
 * que usa la caché inicial de la app Android.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final MuseumRepository museums;
    private final ProductRepository products;
    private final NoticeRepository notices;

    public DataLoader(MuseumRepository museums, ProductRepository products, NoticeRepository notices) {
        this.museums = museums;
        this.products = products;
        this.notices = notices;
    }

    private static String tour(String q) {
        return "https://artsandculture.google.com/search?q=" + q.replace(" ", "%20");
    }

    @Override
    public void run(String... args) {
        if (museums.count() > 0) return;

        List<Museum> list = List.of(
                new Museum("Museo Nacional de Arte Colonial", "Colombia", "Bogotá", 4.5970, -74.0745,
                        "Recorre siglos de historia a través de colecciones de arte colonial iberoamericano. Explora salas virtuales, exposiciones y material educativo.",
                        "Historia", "Patrimonio Colonial Digital", tour("Museo Colonial Bogota"), 4.8),
                new Museum("Museo de Arte Moderno", "México", "Ciudad de México", 19.4229, -99.1773,
                        "Colección de arte mexicano del siglo XX con recorridos virtuales por sus salas circulares.",
                        "Arte moderno", "Arte Moderno en Línea", tour("Museo de Arte Moderno Mexico"), 4.6),
                new Museum("Museo del Prado Virtual", "España", "Madrid", 40.4138, -3.6921,
                        "Visita virtual a obras maestras de la pintura europea. Registro de demostración para el MVP.",
                        "Bellas artes", "Red Iberoamericana", tour("Museo del Prado"), 4.9),
                new Museum("Museo de Arte de Lima", "Perú", "Lima", -12.0605, -77.0369,
                        "Arte peruano desde la época precolombina hasta el presente, con recorridos 360°.",
                        "Historia", "Patrimonio Colonial Digital", tour("Museo de Arte de Lima"), 4.7),
                new Museum("Museo Nacional de Bellas Artes", "Chile", "Santiago", -33.4344, -70.6436,
                        "Colección de arte chileno y universal en un edificio patrimonial del Parque Forestal.",
                        "Bellas artes", "Red Iberoamericana", tour("Museo Nacional de Bellas Artes Chile"), 4.7),
                new Museum("Museo Nacional de Bellas Artes (AR)", "Argentina", "Buenos Aires", -34.5839, -58.3929,
                        "Pintura y escultura argentina y europea con visitas virtuales guiadas.",
                        "Bellas artes", "Arte Moderno en Línea", tour("Museo Nacional de Bellas Artes Buenos Aires"), 4.6),
                new Museum("Museo de Arte de São Paulo", "Brasil", "São Paulo", -23.5614, -46.6559,
                        "Colección internacional exhibida en sus icónicos caballetes de vidrio.",
                        "Arte moderno", "Arte Moderno en Línea", tour("MASP Sao Paulo"), 4.8),
                new Museum("Museo Nacional de Antropología", "México", "Ciudad de México", 19.4260, -99.1863,
                        "Patrimonio arqueológico y etnográfico de Mesoamérica en recorridos virtuales.",
                        "Arqueología", "Red Iberoamericana", tour("Museo Nacional de Antropologia"), 4.9),
                new Museum("Museu Nacional de Arte Antiga", "Portugal", "Lisboa", 38.7050, -9.1610,
                        "Arte portugués y europeo de los siglos XII al XIX.",
                        "Historia", "Red Iberoamericana", tour("Museu Nacional de Arte Antiga"), 4.6),
                new Museum("Centro de Arte Contemporáneo de Quito", "Ecuador", "Quito", -0.2127, -78.5063,
                        "Arte contemporáneo ecuatoriano en un antiguo hospital militar restaurado.",
                        "Arte contemporáneo", "Arte Moderno en Línea", tour("Centro de Arte Contemporaneo Quito"), 4.5),
                new Museum("Museo Nacional de Costa Rica", "Costa Rica", "San José", 9.9326, -84.0716,
                        "Historia natural y cultural costarricense en el antiguo Cuartel Bellavista.",
                        "Historia", "Patrimonio Colonial Digital", tour("Museo Nacional de Costa Rica"), 4.5),
                new Museum("Museo Nacional de Artes Visuales", "Uruguay", "Montevideo", -34.9143, -56.1520,
                        "Principal colección de arte uruguayo, con exposiciones temporales virtuales.",
                        "Bellas artes", "Red Iberoamericana", tour("Museo Nacional de Artes Visuales Montevideo"), 4.4)
        );
        list.forEach(m -> {
            Museum saved = museums.save(m);
            saved.setQrCode("MUSEO:" + saved.getId());
            museums.save(saved);
        });

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

        Notice welcome = new Notice();
        welcome.setTitle("Bienvenida al MVP");
        welcome.setBody("Backend Spring Boot conectado correctamente");
        welcome.setCategory("EVENTO");
        welcome.setDestination("home");
        welcome.setCreatedAt(System.currentTimeMillis());
        notices.save(welcome);
    }
}
