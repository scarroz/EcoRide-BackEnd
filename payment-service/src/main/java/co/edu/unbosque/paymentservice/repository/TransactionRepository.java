package co.edu.unbosque.paymentservice.repository;

import co.edu.unbosque.paymentservice.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findByUserIdAndStatus(Long userId, String status);

    List<Transaction> findByStripePaymentId(String stripePaymentId);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}