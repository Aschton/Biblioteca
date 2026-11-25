package cl.duoc.museo.museos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio de museos virtuales, avisos y resolución de QR de museos (puerto 8081). */
@SpringBootApplication
public class MsMuseosApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsMuseosApplication.class, args);
    }
}
