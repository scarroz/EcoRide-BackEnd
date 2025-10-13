package co.edu.unbosque.paymentservice.dto;

public record PaymentMethodResponseDTO(
        String paymentMethodId,
        String brand,
        String last4,
        boolean isDefault
) {}
