package co.edu.unbosque.tripservice.messaging;

import co.edu.unbosque.tripservice.dto.event.*;
import co.edu.unbosque.tripservice.dto.event.*;
import co.edu.unbosque.tripservice.model.Bicycle;
import co.edu.unbosque.tripservice.model.Trip;
import co.edu.unbosque.tripservice.repository.BicycleRepository;
import co.edu.unbosque.tripservice.repository.TripRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Consumer de eventos IoT desde Kafka
 * Implementa patrón Event-Driven Architecture
 */
@Component
public class IoTEventConsumer {

    private final ObjectMapper objectMapper;
    private final TripRepository tripRepository;
    private final BicycleRepository bicycleRepository;

    public IoTEventConsumer(
            ObjectMapper objectMapper,
            TripRepository tripRepository,
            BicycleRepository bicycleRepository
    ) {
        this.objectMapper = objectMapper;
        this.tripRepository = tripRepository;
        this.bicycleRepository = bicycleRepository;
    }

    // ================================================
    // CONSUMIR TELEMETRÍA
    // ================================================

    @KafkaListener(
            topics = "bicycle-telemetry",
            groupId = "trip-service-iot-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeTelemetry(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            BicycleTelemetryEvent telemetry = objectMapper.readValue(message, BicycleTelemetryEvent.class);

            System.out.println("Telemetría recibida [P:" + partition + "|O:" + offset + "] - " +
                    "Bike: " + telemetry.bikeId() +
                    " | Pos: " + telemetry.latitude() + "," + telemetry.longitude() +
                    " | Speed: " + telemetry.speedKmh() + " km/h" +
                    " | Distance: " + telemetry.distanceFromStart() + " km");

            // Procesar telemetría
            processTelemetry(telemetry);

            // Validar anomalías
            detectAnomalies(telemetry);

            // Commit manual - solo si se procesó correctamente
            acknowledgment.acknowledge();

        } catch (Exception e) {
            System.err.println("Error procesando telemetría: " + e.getMessage());
            // No hacer acknowledge - se reintentará
        }
    }

    // ================================================
    // CONSUMIR ESTADO DE CANDADO
    // ================================================

    @KafkaListener(
            topics = "bicycle-lock-status",
            groupId = "trip-service-iot-group"
    )
    @Transactional
    public void consumeLockStatus(
            @Payload String message,
            Acknowledgment acknowledgment
    ) {
        try {
            LockStatusEvent lockEvent = objectMapper.readValue(message, LockStatusEvent.class);

            System.out.println("Lock status recibido - Bike: " + lockEvent.bikeId() +
                    " -> " + lockEvent.lockStatus());

            // Actualizar estado de bicicleta en BD
            Optional<Bicycle> bicycleOpt = bicycleRepository.findById(lockEvent.bikeId());

            bicycleOpt.ifPresent(bicycle -> {
                String newStatus = "LOCKED".equals(lockEvent.lockStatus()) ?
                        "AVAILABLE" : "IN_USE";

                bicycle.setStatus(newStatus);
                bicycleRepository.save(bicycle);

                System.out.println("Estado de bicicleta actualizado: " + newStatus);
            });

            acknowledgment.acknowledge();

        } catch (Exception e) {
            System.err.println("Error procesando lock status: " + e.getMessage());
        }
    }

    // ================================================
    // CONSUMIR ESTADO DE BATERÍA
    // ================================================

    @KafkaListener(
            topics = "bicycle-battery",
            groupId = "trip-service-iot-group"
    )
    @Transactional
    public void consumeBatteryStatus(
            @Payload String message,
            Acknowledgment acknowledgment
    ) {
        try {
            BatteryStatusEvent batteryEvent = objectMapper.readValue(message, BatteryStatusEvent.class);

            System.out.println("Battery status recibido - Bike: " + batteryEvent.bikeId() +
                    " -> " + batteryEvent.batteryLevel() + "% [" + batteryEvent.alertLevel() + "]");

            // Actualizar nivel de batería en BD
            Optional<Bicycle> bicycleOpt = bicycleRepository.findById(batteryEvent.bikeId());

            bicycleOpt.ifPresent(bicycle -> {
                bicycle.setBatteryLevel(batteryEvent.batteryLevel());
                bicycleRepository.save(bicycle);
            });

            // Si batería crítica, marcar bicicleta para mantenimiento
            if ("CRITICAL".equals(batteryEvent.alertLevel())) {
                handleCriticalBattery(batteryEvent);
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            System.err.println("Error procesando battery status: " + e.getMessage());
        }
    }

    // ================================================
    // CONSUMIR ALERTAS
    // ================================================

    @KafkaListener(
            topics = "bicycle-alerts",
            groupId = "trip-service-iot-group"
    )
    public void consumeAlerts(
            @Payload String message,
            Acknowledgment acknowledgment
    ) {
        try {
            BicycleAlertEvent alert = objectMapper.readValue(message, BicycleAlertEvent.class);

            System.out.println("ALERTA [" + alert.severity() + "] - " +
                    "Tipo: " + alert.alertType() +
                    " | Bike: " + alert.bikeId() +
                    " | Mensaje: " + alert.message());

            // Procesar según tipo de alerta
            switch (alert.alertType()) {
                case "LOW_BATTERY" -> handleLowBatteryAlert(alert);
                case "OUT_OF_ZONE" -> handleOutOfZoneAlert(alert);
                case "SPEED_LIMIT" -> handleSpeedLimitAlert(alert);
                case "ABANDONED" -> handleAbandonedBikeAlert(alert);
                default -> System.out.println("Tipo de alerta desconocido: " + alert.alertType());
            }

            // TODO: Enviar notificaciones a usuarios/administradores
            // TODO: Crear tickets de mantenimiento si es necesario

            acknowledgment.acknowledge();

        } catch (Exception e) {
            System.err.println("Error procesando alerta: " + e.getMessage());
        }
    }

    // ================================================
    // CONSUMIR EVENTOS DE VIAJE (Event Sourcing)
    // ================================================

    @KafkaListener(
            topics = "trip-events",
            groupId = "trip-service-analytics-group"
    )
    public void consumeTripEvents(
            @Payload String message,
            Acknowledgment acknowledgment
    ) {
        try {
            // Detectar tipo de evento
            if (message.contains("\"trip_type\"")) {
                TripStartedEvent event = objectMapper.readValue(message, TripStartedEvent.class);
                System.out.println("🚴 TripStarted consumido - Trip: " + event.tripId());
                // Procesar para analytics, reporting, etc.
            } else if (message.contains("\"distance_km\"")) {
                TripCompletedEvent event = objectMapper.readValue(message, TripCompletedEvent.class);
                System.out.println("🏁 TripCompleted consumido - Trip: " + event.tripId() +
                        " | Distance: " + event.distanceKm() + " km" +
                        " | Cost: $" + event.cost());
                // Procesar para analytics, facturación, reportes
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            System.err.println("Error procesando trip event: " + e.getMessage());
        }
    }

    // ================================================
    // MÉTODOS DE PROCESAMIENTO
    // ================================================

    private void processTelemetry(BicycleTelemetryEvent telemetry) {
        // Actualizar posición actual del viaje en BD (opcional)
        Optional<Trip> tripOpt = tripRepository.findById(telemetry.tripId());

        tripOpt.ifPresent(trip -> {
            // Aquí podrías guardar waypoints, actualizar distancia en tiempo real, etc.
            // Por ahora solo logeamos
            System.out.println("   → Viaje " + trip.getId() + " en progreso: " +
                    telemetry.distanceFromStart() + " km recorridos");
        });
    }

    private void detectAnomalies(BicycleTelemetryEvent telemetry) {
        // Velocidad excesiva (>40 km/h sospechoso)
        if (telemetry.speedKmh().compareTo(BigDecimal.valueOf(40)) > 0) {
            System.out.println("⚠️ ANOMALÍA: Velocidad excesiva - " + telemetry.speedKmh() + " km/h");
            // TODO: Generar alerta SPEED_LIMIT
        }

        // Precisión GPS muy baja (>20m)
        if (telemetry.accuracy().compareTo(BigDecimal.valueOf(20)) > 0) {
            System.out.println("⚠️ ANOMALÍA: Señal GPS débil - Precisión: " + telemetry.accuracy() + "m");
        }

        // TODO: Validar que la bicicleta esté dentro de los límites de Bogotá
        // TODO: Detectar si está en una zona no permitida
    }

    private void handleCriticalBattery(BatteryStatusEvent batteryEvent) {
        Optional<Bicycle> bicycleOpt = bicycleRepository.findById(batteryEvent.bikeId());

        bicycleOpt.ifPresent(bicycle -> {
            if ("IN_USE".equals(bicycle.getStatus())) {
                System.out.println("⚠️ Bicicleta " + bicycle.getId() +
                        " en uso con batería crítica (" + batteryEvent.batteryLevel() + "%)");
                // TODO: Notificar al usuario que debe terminar el viaje pronto
            } else {
                // Marcar para mantenimiento/carga
                bicycle.setStatus("MAINTENANCE");
                bicycleRepository.save(bicycle);
                System.out.println("🔧 Bicicleta " + bicycle.getId() +
                        " marcada para mantenimiento (batería crítica)");
            }
        });
    }

    private void handleLowBatteryAlert(BicycleAlertEvent alert) {
        System.out.println("   → Procesando alerta de batería baja...");
        // TODO: Notificar al usuario si está en viaje
        // TODO: Sugerir estaciones cercanas para finalizar viaje
    }

    private void handleOutOfZoneAlert(BicycleAlertEvent alert) {
        System.out.println("   → Bicicleta fuera de zona permitida");
        // TODO: Notificar al usuario
        // TODO: Aplicar penalización si es necesario
    }

    private void handleSpeedLimitAlert(BicycleAlertEvent alert) {
        System.out.println("   → Exceso de velocidad detectado");
        // TODO: Registrar incidente
    }

    private void handleAbandonedBikeAlert(BicycleAlertEvent alert) {
        System.out.println("   → Posible bicicleta abandonada");
    }
}