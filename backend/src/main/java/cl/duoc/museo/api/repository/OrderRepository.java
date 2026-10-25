package cl.duoc.museo.api.repository;

import cl.duoc.museo.api.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByUserEmailIgnoreCaseOrderByCreatedAtDesc(String userEmail);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
}
