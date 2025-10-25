package co.edu.unbosque.tripservice.service.impl;

import co.edu.unbosque.tripservice.client.*;
import co.edu.unbosque.tripservice.dto.*;
import co.edu.unbosque.tripservice.mapper.DataMapper;
import co.edu.unbosque.tripservice.model.*;
import co.edu.unbosque.tripservice.repository.*;
import co.edu.unbosque.tripservice.service.OSRMRouteService;
import co.edu.unbosque.tripservice.service.RouteValidationService;
import co.edu.unbosque.tripservice.service.TripService;
import co.edu.unbosque.tripservice.util.IoTBikeSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepo;
    private final BicycleRepository bicycleRepo;
    private final StationRepository stationRepo;
    private final ReservationRepository reservationRepo;
    private final DataMapper mapper;
    private final UserServiceClient userClient;
    private final PaymentServiceClient paymentClient;
    private final IoTBikeSimulator iotSimulator;
    private final RouteValidationService routeValidation;
    private final OSRMRouteService osrmService;

    @Value("${trip.cost.last-mile.base}")
    private BigDecimal lastMileBaseCost;

    @Value("${trip.cost.last-mile.extra-minute}")
    private BigDecimal lastMileExtraMinute;

    @Value("${trip.cost.last-mile.max-minutes}")
    private int lastMileMaxMinutes;

    @Value("${trip.cost.long-distance.base}")
    private BigDecimal longDistanceBaseCost;

    @Value("${trip.cost.long-distance.extra-minute}")
    private BigDecimal longDistanceExtraMinute;

    @Value("${trip.cost.long-distance.max-minutes}")
    private int longDistanceMaxMinutes;

    public TripServiceImpl(
            TripRepository tripRepo,
            BicycleRepository bicycleRepo,
            StationRepository stationRepo,
            ReservationRepository reservationRepo,
            DataMapper mapper,
            UserServiceClient userClient,
            PaymentServiceClient paymentClient,
            IoTBikeSimulator iotSimulator,
            RouteValidationService routeValidation,
            OSRMRouteService osrmService
    ) {
        this.tripRepo = tripRepo;
        this.bicycleRepo = bicycleRepo;
        this.stationRepo = stationRepo;
        this.reservationRepo = reservationRepo;
        this.mapper = mapper;
        this.userClient = userClient;
        this.paymentClient = paymentClient;
        this.iotSimulator = iotSimulator;
        this.routeValidation = routeValidation;
        this.osrmService = osrmService;
    }

    @Override
    @Transactional
    public TripResponseDTO startTrip(TripStartRequestDTO request) {
        System.out.println("Iniciando viaje para usuario: " + request.userId());

        // 1. Validar usuario
        UserValidationResponseDTO userValidation = userClient.validateUser(request.userId());
        if (!userValidation.valid()) {
            throw new RuntimeException("Usuario inválido o con saldo insuficiente");
        }

        // 2. Verificar que no tenga viajes activos
        tripRepo.findByUserIdAndStatus(request.userId(), "IN_PROGRESS")
                .ifPresent(t -> {
                    throw new RuntimeException("Usuario ya tiene un viaje activo");
                });

        // 3. Obtener bicicleta
        Bicycle bicycle = bicycleRepo.findById(request.bicycleId())
                .orElseThrow(() -> new RuntimeException("Bicicleta no encontrada"));

        // 4. Validar disponibilidad de bicicleta
        validateBicycleAvailability(bicycle);

        // 5. Obtener estación de inicio
        Station startStation = stationRepo.findById(request.stationId())
                .orElseThrow(() -> new RuntimeException("Estación no encontrada"));

        // 6. Verificar reserva activa si existe
        reservationRepo.findByUserIdAndStatus(request.userId(), "ACTIVE")
                .ifPresent(reservation -> {
                    if (!reservation.getBicycle().getId().equals(bicycle.getId())) {
                        throw new RuntimeException("La bicicleta no coincide con la reserva");
                    }
                    reservation.setStatus("USED");
                    reservationRepo.save(reservation);
                });

        // 7. Validar tipo de viaje
        validateTripType(request.tripType(), startStation);

        // 8. Crear viaje
        Trip trip = mapper.toTripEntity(
                request.userId(),
                bicycle,
                startStation,
                request.tripType(),
                request.paymentSource()
        );

        // 9. Actualizar estado de bicicleta
        bicycle.setStatus("IN_USE");
        bicycleRepo.save(bicycle);

        // 10. Guardar viaje
        trip = tripRepo.save(trip);

        System.out.println("Viaje creado con ID: " + trip.getId());

        // 11. INICIAR SIMULACIÓN AUTOMÁTICA IOT
        iotSimulator.startSimulation(trip);

        // 12. Notificar a usuario
        userClient.notifyTripStarted(request.userId(), trip.getId());

        return mapper.toTripResponseDTO(trip);
    }

    @Override
    @Transactional
    public TripResponseDTO endTrip(TripEndRequestDTO request) {
        System.out.println("Finalizando viaje: " + request.tripId());

        // 1. Obtener viaje
        Trip trip = tripRepo.findById(request.tripId())
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        if (!"IN_PROGRESS".equals(trip.getStatus())) {
            throw new RuntimeException("El viaje no está activo");
        }

        // 2. DETENER SIMULACIÓN IOT
        iotSimulator.stopSimulation(trip.getId());

        // 3. Obtener estación de finalización
        Station endStation = stationRepo.findById(request.endStationId())
                .orElseThrow(() -> new RuntimeException("Estación de destino no encontrada"));

        // 4. Validar capacidad de estación
        Integer availableSlots = endStation.getCapacity() -
                bicycleRepo.countAvailableBicyclesByStation(endStation.getId());

        if (availableSlots <= 0) {
            throw new RuntimeException("La estación destino está llena");
        }

        // 5. Validar posición GPS (si se proporciona)
        if (request.finalLatitude() != null && request.finalLongitude() != null) {
            RouteValidationResponseDTO routeValidation = this.routeValidation.validateFinalPosition(
                    endStation.getLatitude(),
                    endStation.getLongitude(),
                    request.finalLatitude(),
                    request.finalLongitude()
            );

            if (!routeValidation.valid()) {
                throw new RuntimeException("La posición GPS no coincide con la estación");
            }
        }

        // 6. Calcular duración y costo
        trip.setEndTime(LocalDateTime.now());
        trip.setEndStation(endStation);

        long durationMinutes = Duration.between(trip.getStartTime(), trip.getEndTime()).toMinutes();

        // Calcular distancia real usando OSRM si es posible
        BigDecimal distanceKm;
        try {
            var osrmRoute = osrmService.getRoute(
                    trip.getStartStation().getLongitude(),
                    trip.getStartStation().getLatitude(),
                    endStation.getLongitude(),
                    endStation.getLatitude()
            );
            distanceKm = osrmRoute.totalDistance();
            System.out.println("Distancia calculada con OSRM: " + distanceKm + " km");
        } catch (Exception e) {
            distanceKm = calculateHaversineDistance(
                    trip.getStartStation().getLatitude(),
                    trip.getStartStation().getLongitude(),
                    endStation.getLatitude(),
                    endStation.getLongitude()
            );
            System.out.println("OSRM falló, usando Haversine: " + distanceKm + " km");
        }

        trip.setDistanceKm(distanceKm);

        BigDecimal totalCost = calculateTripCost(trip.getTripType(), durationMinutes);
        trip.setTotalCost(totalCost);

        // 7. Actualizar estado de bicicleta
        Bicycle bicycle = trip.getBicycle();
        bicycle.setStatus("AVAILABLE");
        bicycle.setLastStation(endStation);
        bicycleRepo.save(bicycle);

        // 8. Procesar pago
        TripPaymentRequestDTO paymentRequest = new TripPaymentRequestDTO(
                trip.getUserId(),
                trip.getId(),
                totalCost,
                trip.getPaymentSource()
        );

        TripPaymentResponseDTO paymentResponse = paymentClient.processTripPayment(paymentRequest);

        if (!"COMPLETED".equals(paymentResponse.status())) {
            trip.setStatus("PAYMENT_FAILED");
            tripRepo.save(trip);
            throw new RuntimeException("Error al procesar el pago del viaje");
        }

        // 9. Actualizar estado del viaje
        trip.setStatus("COMPLETED");
        trip = tripRepo.save(trip);

        System.out.println("Viaje finalizado exitosamente. Costo: $" + totalCost);

        // 10. Notificar finalización
        userClient.notifyTripCompleted(trip.getUserId(), trip.getId(), totalCost);

        return mapper.toTripResponseDTO(trip);
    }

    @Override
    public List<TripResponseDTO> getUserTrips(Long userId) {
        return tripRepo.findByUserIdOrderByStartTimeDesc(userId)
                .stream()
                .map(mapper::toTripResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TripDetailDTO getTripDetail(Long tripId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Viaje no encontrado"));

        return mapper.toTripDetailDTO(trip, "User Name");
    }

    @Override
    public List<TripResponseDTO> getActiveTrips() {
        return tripRepo.findByStatus("IN_PROGRESS")
                .stream()
                .map(mapper::toTripResponseDTO)
                .collect(Collectors.toList());
    }

    // ================================================
    // MÉTODOS PRIVADOS AUXILIARES
    // ================================================

    private void validateBicycleAvailability(Bicycle bicycle) {
        if (!"AVAILABLE".equals(bicycle.getStatus()) && !"RESERVED".equals(bicycle.getStatus())) {
            throw new RuntimeException("La bicicleta no está disponible");
        }

        if ("ELECTRIC".equals(bicycle.getType())) {
            if (bicycle.getBatteryLevel() == null || bicycle.getBatteryLevel() < 40) {
                throw new RuntimeException("La bicicleta eléctrica tiene batería baja");
            }
        }
    }

    private void validateTripType(String tripType, Station startStation) {
        if (!"LAST_MILE".equals(tripType) && !"LONG_DISTANCE".equals(tripType)) {
            throw new RuntimeException("Tipo de viaje inválido");
        }

        if ("LAST_MILE".equals(tripType)) {
            if (!startStation.getName().contains("Metro")) {
                throw new RuntimeException("Los viajes de última milla deben iniciar en estaciones Metro");
            }
        }
    }

    private BigDecimal calculateTripCost(String tripType, long durationMinutes) {
        BigDecimal baseCost;
        BigDecimal extraMinuteCost;
        int maxMinutes;

        if ("LAST_MILE".equals(tripType)) {
            baseCost = lastMileBaseCost;
            extraMinuteCost = lastMileExtraMinute;
            maxMinutes = lastMileMaxMinutes;
        } else {
            baseCost = longDistanceBaseCost;
            extraMinuteCost = longDistanceExtraMinute;
            maxMinutes = longDistanceMaxMinutes;
        }

        if (durationMinutes <= maxMinutes) {
            return baseCost;
        }

        long extraMinutes = durationMinutes - maxMinutes;
        BigDecimal extraCost = extraMinuteCost.multiply(BigDecimal.valueOf(extraMinutes));

        return baseCost.add(extraCost);
    }

    private BigDecimal calculateHaversineDistance(
            BigDecimal lat1, BigDecimal lon1,
            BigDecimal lat2, BigDecimal lon2
    ) {
        final double EARTH_RADIUS_KM = 6371.0;

        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1.doubleValue())) *
                        Math.cos(Math.toRadians(lat2.doubleValue())) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return BigDecimal.valueOf(EARTH_RADIUS_KM * c)
                .setScale(2, RoundingMode.HALF_UP);
    }
}