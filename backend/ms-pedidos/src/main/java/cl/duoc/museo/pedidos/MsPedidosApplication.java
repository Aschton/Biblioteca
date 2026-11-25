package cl.duoc.museo.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio de pedidos, solicitudes de información y cálculo de envío (puerto 8083). */
@SpringBootApplication
public class MsPedidosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsPedidosApplication.class, args);
    }
}
