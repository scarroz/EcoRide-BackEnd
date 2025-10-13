package co.edu.unbosque.paymentservice.repository;

import co.edu.unbosque.paymentservice.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {}
