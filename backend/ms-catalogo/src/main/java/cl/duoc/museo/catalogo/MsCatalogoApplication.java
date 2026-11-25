package cl.duoc.museo.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio del catálogo de productos y cursos (puerto 8082). */
@SpringBootApplication
public class MsCatalogoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsCatalogoApplication.class, args);
    }
}
