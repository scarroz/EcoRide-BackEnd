package co.edu.unbosque.paymentservice.dto;

public record SubscriptionPlanResponseDTO(
        Long id,
        String name,
        Double price,
        Integer maxTrips,
        Boolean active
) {}