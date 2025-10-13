package co.edu.unbosque.paymentservice.dto;

public record SubscriptionPaymentRequestDTO(
        Long userId,
        Long planId,
        String paymentMethodId  // Token de Stripe
) {}
