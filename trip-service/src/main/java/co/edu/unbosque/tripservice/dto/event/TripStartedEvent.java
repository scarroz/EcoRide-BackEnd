package co.edu.unbosque.tripservice.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TripStartedEvent(
        @JsonProperty("trip_id") Long tripId,
        @JsonProperty("user_id") Long userId,
        @JsonProperty("bike_id") Long bikeId,
        @JsonProperty("station_id") Long stationId,
        @JsonProperty("trip_type") String tripType,
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("start_location") LocationData startLocation
) {}
