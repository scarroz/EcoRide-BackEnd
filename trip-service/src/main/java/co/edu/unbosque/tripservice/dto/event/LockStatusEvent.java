package co.edu.unbosque.tripservice.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LockStatusEvent(
        @JsonProperty("bike_id") Long bikeId,
        @JsonProperty("lock_status") String lockStatus, // LOCKED, UNLOCKED
        @JsonProperty("station_id") Long stationId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("location") LocationData location
) {}
