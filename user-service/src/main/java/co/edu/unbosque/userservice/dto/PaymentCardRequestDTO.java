package co.edu.unbosque.userservice.dto;

public record PaymentCardRequestDTO(
        Long userId,
        String name,
        String email,
        String paymentMethodId
) {}
