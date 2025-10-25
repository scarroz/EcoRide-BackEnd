package co.edu.unbosque.tripservice.mapper;


import co.edu.unbosque.tripservice.dto.*;
import co.edu.unbosque.tripservice.model.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class DataMapper {

    // ========================================
    // STATION MAPPINGS
    // ========================================

    public Station toStationEntity(StationRequestDTO dto) {
        if (dto == null) return null;

        Station station = new Station();
        station.setName(dto.name());
        station.setLatitude(dto.latitude());
        station.setLongitude(dto.longitude());
        station.setCapacity(dto.capacity());
        station.setActive(true);

        return station;
    }

    public StationResponseDTO toStationResponseDTO(Station entity, Integer availableBicycles) {
        if (entity == null) return null;

        return new StationResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getCapacity(),
                entity.getActive(),
                availableBicycles
        );
    }

    // ========================================
    // BICYCLE MAPPINGS
    // ========================================

    public Bicycle toBicycleEntity(BicycleRequestDTO dto, Station station) {
        if (dto == null) return null;

        Bicycle bicycle = new Bicycle();
        bicycle.setCode(dto.code());
        bicycle.setType(dto.type());
        bicycle.setStatus("AVAILABLE");
        bicycle.setLastStation(station);

        if ("ELECTRIC".equals(dto.type())) {
            bicycle.setBatteryLevel(100);
        }

        return bicycle;
    }

    public BicycleResponseDTO toBicycleResponseDTO(Bicycle entity) {
        if (entity == null) return null;

        return new BicycleResponseDTO(
                entity.getId(),
                entity.getCode(),
                entity.getType(),
                entity.getBatteryLevel(),
                entity.getStatus(),
                entity.getLastStation() != null ? entity.getLastStation().getId() : null,
                entity.getLastStation() != null ? entity.getLastStation().getName() : null
        );
    }

    public void updateBicycleFromDTO(Bicycle entity, BicycleStatusUpdateDTO dto, Station station) {
        if (entity == null || dto == null) return;

        if (dto.status() != null) {
            entity.setStatus(dto.status());
        }
        if (dto.batteryLevel() != null) {
            entity.setBatteryLevel(dto.batteryLevel());
        }
        if (station != null) {
            entity.setLastStation(station);
        }
    }

    // mapeo de reservacion

    public Reservation toReservationEntity(Long userId, Bicycle bicycle, Station station) {
        if (bicycle == null || station == null) return null;

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setBicycle(bicycle);
        reservation.setStation(station);
        reservation.setReservedAt(LocalDateTime.now());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        reservation.setStatus("ACTIVE");

        return reservation;
    }

    public ReservationResponseDTO toReservationResponseDTO(Reservation entity) {
        if (entity == null) return null;

        return new ReservationResponseDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getBicycle().getId(),
                entity.getBicycle().getCode(),
                entity.getStation().getId(),
                entity.getStation().getName(),
                entity.getReservedAt(),
                entity.getExpiresAt(),
                entity.getStatus()
        );
    }

    //mapeo de trip

    public Trip toTripEntity(Long userId, Bicycle bicycle, Station startStation, String tripType, String paymentSource) {
        if (bicycle == null) return null;

        Trip trip = new Trip();
        trip.setUserId(userId);
        trip.setBicycle(bicycle);
        trip.setStartStation(startStation);
        trip.setStartTime(LocalDateTime.now());
        trip.setStatus("IN_PROGRESS");
        trip.setTripType(tripType);
        trip.setPaymentSource(paymentSource);

        return trip;
    }

    public TripResponseDTO toTripResponseDTO(Trip entity) {
        if (entity == null) return null;

        Long durationMinutes = null;
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            durationMinutes = Duration.between(entity.getStartTime(), entity.getEndTime()).toMinutes();
        }

        return new TripResponseDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getBicycle().getId(),
                entity.getBicycle().getCode(),
                entity.getStartStation() != null ? entity.getStartStation().getId() : null,
                entity.getStartStation() != null ? entity.getStartStation().getName() : null,
                entity.getEndStation() != null ? entity.getEndStation().getId() : null,
                entity.getEndStation() != null ? entity.getEndStation().getName() : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDistanceKm(),
                entity.getTotalCost(),
                entity.getPaymentSource(),
                entity.getStatus(),
                entity.getTripType(),
                durationMinutes
        );
    }

    public TripDetailDTO toTripDetailDTO(Trip entity, String userName) {
        if (entity == null) return null;

        Long durationMinutes = null;
        if (entity.getStartTime() != null && entity.getEndTime() != null) {
            durationMinutes = Duration.between(entity.getStartTime(), entity.getEndTime()).toMinutes();
        }

        return new TripDetailDTO(
                entity.getId(),
                entity.getUserId(),
                userName,
                toBicycleResponseDTO(entity.getBicycle()),
                entity.getStartStation() != null ? toStationResponseDTO(entity.getStartStation(), null) : null,
                entity.getEndStation() != null ? toStationResponseDTO(entity.getEndStation(), null) : null,
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getDistanceKm(),
                entity.getTotalCost(),
                entity.getPaymentSource(),
                entity.getStatus(),
                entity.getTripType(),
                durationMinutes
        );
    }

    //mapeo de iot

    public BikeIoTDataDTO toBikeIoTDataDTO(Bicycle bicycle) {
        if (bicycle == null) return null;

        return new BikeIoTDataDTO(
                bicycle.getId(),
                LocalDateTime.now().toString(),
                bicycle.getLastStation() != null ? bicycle.getLastStation().getLatitude() : null,
                bicycle.getLastStation() != null ? bicycle.getLastStation().getLongitude() : null,
                bicycle.getBatteryLevel(),
                "AVAILABLE".equals(bicycle.getStatus()) ? "LOCKED" : "UNLOCKED"
        );
    }

    public LockStatusDTO toLockStatusDTO(Long bicycleId, String status) {
        return new LockStatusDTO(bicycleId, status, LocalDateTime.now());
    }
}
