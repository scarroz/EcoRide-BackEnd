package co.edu.unbosque.tripservice.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BicycleAlertEvent(
        @JsonProperty("bike_id") Long bikeId,
        @JsonProperty("trip_id") Long tripId,
        @JsonProperty("alert_type") String alertType, // LOW_BATTERY, OUT_OF_ZONE, SPEED_LIMIT, ABANDONED
        @JsonProperty("severity") String severity, // INFO, WARNING, CRITICAL
        @JsonProperty("message") String message,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("location") LocationData location,
        @JsonProperty("metadata") java.util.Map<String, Object> metadata
) {}
