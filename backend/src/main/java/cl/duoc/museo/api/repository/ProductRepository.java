package cl.duoc.museo.api.repository;

import cl.duoc.museo.api.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByQrCode(String qrCode);
}
