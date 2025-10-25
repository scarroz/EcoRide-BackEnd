package co.edu.unbosque.tripservice.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record BatteryStatusEvent(
        @JsonProperty("bike_id") Long bikeId,
        @JsonProperty("battery_level") Integer batteryLevel,
        @JsonProperty("charging") Boolean charging,
        @JsonProperty("estimated_range_km") BigDecimal estimatedRangeKm,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("alert_level") String alertLevel // OK, LOW, CRITICAL
) {}
