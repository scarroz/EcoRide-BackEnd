package co.edu.unbosque.paymentservice.dto;

public record PaymentCardRequestDTO(
        Long userId,
        String name,
        String email,
        String paymentMethodId
) {}
