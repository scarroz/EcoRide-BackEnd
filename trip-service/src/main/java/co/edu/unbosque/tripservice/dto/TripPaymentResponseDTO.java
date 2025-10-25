package co.edu.unbosque.tripservice.dto;

public record TripPaymentResponseDTO(
        String transactionId,
        String status,
        String message
) {}
