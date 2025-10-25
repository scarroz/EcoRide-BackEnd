package co.edu.unbosque.tripservice.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record BicycleTelemetryEvent(
        @JsonProperty("bike_id") Long bikeId,
        @JsonProperty("trip_id") Long tripId,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("latitude") BigDecimal latitude,
        @JsonProperty("longitude") BigDecimal longitude,
        @JsonProperty("speed_kmh") BigDecimal speedKmh,
        @JsonProperty("bearing") Integer bearing, // 0-360 grados
        @JsonProperty("altitude") BigDecimal altitude,
        @JsonProperty("accuracy") BigDecimal accuracy, // metros
        @JsonProperty("battery_level") Integer batteryLevel,
        @JsonProperty("lock_status") String lockStatus,
        @JsonProperty("distance_from_start") BigDecimal distanceFromStart
) {
    public BicycleTelemetryEvent {
        if (bikeId == null || timestamp == null) {
            throw new IllegalArgumentException("bikeId y timestamp son requeridos");
        }
    }
}
