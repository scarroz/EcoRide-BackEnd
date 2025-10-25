package co.edu.unbosque.tripservice.dto;

import java.math.BigDecimal;

public record TripStatisticsDTO(
        Long totalTrips,
        BigDecimal totalRevenue,
        BigDecimal totalDistanceKm,
        BigDecimal averageTripDuration,
        String mostUsedBicycleType,
        String mostUsedStation
) {}
