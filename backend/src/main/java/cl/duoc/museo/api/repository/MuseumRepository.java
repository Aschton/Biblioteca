package cl.duoc.museo.api.repository;

import cl.duoc.museo.api.model.Museum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MuseumRepository extends JpaRepository<Museum, Long> {
    Optional<Museum> findByQrCode(String qrCode);
}
