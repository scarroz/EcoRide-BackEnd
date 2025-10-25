package co.edu.unbosque.tripservice.service;

import co.edu.unbosque.tripservice.model.Trip;

import java.math.BigDecimal;

/**
 * Servicio encargado de la simulación IoT y publicación de eventos Kafka
 * para bicicletas, viajes y telemetría en tiempo real.
 */
public interface IoTPublisherService {

    /**
     * Inicia la simulación de telemetría para un viaje.
     * Obtiene la ruta desde OSRM y comienza a publicar posiciones periódicamente.
     *
     * @param trip instancia del viaje con información de bicicleta, usuario y estación inicial
     */
    void startTripTelemetry(Trip trip);

    /**
     * Detiene la simulación de telemetría de un viaje en curso,
     * eliminándolo del mapa activo y publicando el evento de finalización.
     *
     * @param tripId     identificador del viaje
     * @param distanceKm distancia total recorrida (en km)
     * @param cost       costo total del viaje
     */
    void stopTripTelemetry(Long tripId, BigDecimal distanceKm, BigDecimal cost);

    /**
     * Publica telemetría activa cada 5 segundos para todos los viajes simulados.
     * Este método normalmente es ejecutado automáticamente mediante @Scheduled,
     * pero puede ser invocado manualmente en pruebas o validaciones.
     */
    void publishActiveTripsTelemetry();
}
