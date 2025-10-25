package co.edu.unbosque.tripservice.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record TripCompletedEvent(
        @JsonProperty("trip_id") Long tripId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("bike_id") Long bikeId,
        @JsonProperty("end_station_id") Long endStationId,
        @JsonProperty("distance_km") BigDecimal distanceKm,
        @JsonProperty("duration_minutes") Long durationMinutes,
        @JsonProperty("cost") BigDecimal cost,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("end_location") LocationData endLocation
) {}
