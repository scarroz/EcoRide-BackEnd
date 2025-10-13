package co.edu.unbosque.paymentservice.mapper;

import co.edu.unbosque.paymentservice.dto.*;
import co.edu.unbosque.paymentservice.model.PaymentMethod;
import co.edu.unbosque.paymentservice.model.SubscriptionPlan;
import co.edu.unbosque.paymentservice.model.Transaction;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper para convertir entre DTOs y Entidades
 * Mantiene la lógica de mapeo centralizada y reutilizable
 */
@Component
public class DataMapper {

    // ========================================
    // PAYMENT METHOD MAPPINGS
    // ========================================

    /**
     * Convierte entidad PaymentMethod a DTO de respuesta
     */
    public PaymentMethodResponseDTO toPaymentMethodResponseDTO(PaymentMethod entity) {
        if (entity == null) return null;

        return new PaymentMethodResponseDTO(
                entity.getTokenId(),
                entity.getCardBrand(),
                entity.getCardLast4(),
                entity.isDefault()
        );
    }

    /**
     * Crea una entidad PaymentMethod desde request DTO y datos de Stripe
     */
    public PaymentMethod toPaymentMethodEntity(
            Long userId,
            String stripeCustomerId,
            String tokenId,
            String cardBrand,
            String cardLast4,
            String type
    ) {
        PaymentMethod method = new PaymentMethod();
        method.setUserId(userId);
        method.setStripeCustomerId(stripeCustomerId);
        method.setTokenId(tokenId);
        method.setCardBrand(cardBrand);
        method.setCardLast4(cardLast4);
        method.setType(type);
        method.setActive(true);
        method.setDefault(false);
        method.setCreatedAt(LocalDateTime.now());
        return method;
    }

    // ========================================
    // TRANSACTION MAPPINGS
    // ========================================

    /**
     * Crea una transacción inicial (PENDING) desde TransactionCreateDTO
     */
    public Transaction toTransactionEntity(TransactionCreateDTO dto) {
        if (dto == null) return null;

        Transaction transaction = new Transaction();
        transaction.setUserId(dto.userId());
        transaction.setTripId(dto.tripId());
        transaction.setPaymentMethodId(dto.paymentMethodId());
        transaction.setWalletId(dto.walletId());
        transaction.setAmount(dto.amount());
        transaction.setType(dto.type());
        transaction.setSource(dto.source());
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * Crea una transacción de recarga de wallet
     */
    public Transaction createWalletRechargeTransaction(
            Long userId,
            Long paymentMethodId,
            java.math.BigDecimal amount
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setPaymentMethodId(paymentMethodId);
        transaction.setAmount(amount);
        transaction.setType("TOP_UP");
        transaction.setSource("CARD");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * Crea una transacción de pago de viaje
     */
    public Transaction createTripPaymentTransaction(
            Long userId,
            Long tripId,
            Long walletId,
            java.math.BigDecimal amount
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setTripId(tripId);
        transaction.setWalletId(walletId);
        transaction.setAmount(amount);
        transaction.setType("TRIP_PAYMENT");
        transaction.setSource("WALLET");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * Crea una transacción de suscripción
     */
    public Transaction createSubscriptionTransaction(
            Long userId,
            Long paymentMethodId,
            java.math.BigDecimal amount
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(userId);
        transaction.setPaymentMethodId(paymentMethodId);
        transaction.setAmount(amount);
        transaction.setType("SUBSCRIPTION");
        transaction.setSource("CARD");
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());
        return transaction;
    }

    /**
     * Convierte entidad Transaction a DTO de respuesta simple
     */
    public TransactionResponseDTO toTransactionResponseDTO(Transaction entity) {
        if (entity == null) return null;

        return new TransactionResponseDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getType(),
                entity.getSource(),
                entity.getStatus(),
                entity.getStripePaymentId(),
                entity.getCreatedAt()
        );
    }

    /**
     * Convierte entidad Transaction a DTO detallado (con info de PaymentMethod)
     */
    public TransactionDetailDTO toTransactionDetailDTO(
            Transaction entity,
            PaymentMethod paymentMethod
    ) {
        if (entity == null) return null;

        return new TransactionDetailDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getTripId(),
                entity.getPaymentMethodId(),
                entity.getWalletId(),
                entity.getAmount(),
                entity.getType(),
                entity.getSource(),
                entity.getStatus(),
                entity.getStripePaymentId(),
                entity.getCreatedAt(),
                paymentMethod != null ? paymentMethod.getCardBrand() : null,
                paymentMethod != null ? paymentMethod.getCardLast4() : null
        );
    }

    // ========================================
    // SUBSCRIPTION PLAN MAPPINGS
    // ========================================

    /**
     * Convierte entidad SubscriptionPlan a DTO de respuesta
     */
    public SubscriptionPlanResponseDTO toSubscriptionPlanResponseDTO(SubscriptionPlan entity) {
        if (entity == null) return null;

        return new SubscriptionPlanResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getMaxTrips(),
                entity.isActive()
        );
    }

    // ========================================
    // NOTIFICATION MAPPINGS
    // ========================================

    /**
     * Crea DTO de notificación de recarga de wallet
     */
    public WalletNotificationDTO toWalletNotificationDTO(
            Long userId,
            java.math.BigDecimal amount
    ) {
        return new WalletNotificationDTO(userId, amount);
    }

    /**
     * Crea DTO de notificación de pago de viaje
     */
    public TripPaymentNotificationDTO toTripPaymentNotificationDTO(
            Long tripId,
            Long userId,
            java.math.BigDecimal amount,
            String transactionId
    ) {
        return new TripPaymentNotificationDTO(tripId, userId, amount, transactionId);
    }
}