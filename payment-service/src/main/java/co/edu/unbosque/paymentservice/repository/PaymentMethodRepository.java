package co.edu.unbosque.paymentservice.repository;

import co.edu.unbosque.paymentservice.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUserIdAndActiveTrue(Long userId);

    Optional<PaymentMethod> findByTokenId(String tokenId);
}
