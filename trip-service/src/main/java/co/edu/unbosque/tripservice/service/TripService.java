package co.edu.unbosque.tripservice.service;

import co.edu.unbosque.tripservice.dto.TripStartRequestDTO;
import co.edu.unbosque.tripservice.dto.TripEndRequestDTO;
import co.edu.unbosque.tripservice.dto.TripResponseDTO;
import co.edu.unbosque.tripservice.dto.TripDetailDTO;

import java.util.List;

public interface TripService {

    /**
     * Inicia un viaje para un usuario con una bicicleta específica.
     * Valida usuario, disponibilidad de bicicleta, reserva y tipo de viaje.
     *
     * @param request datos del inicio del viaje
     * @return información del viaje creado
     */
    TripResponseDTO startTrip(TripStartRequestDTO request);

    /**
     * Finaliza un viaje activo.
     * Calcula duración, costo, distancia (OSRM o Haversine),
     * procesa el pago y libera la bicicleta.
     *
     * @param request datos de finalización del viaje
     * @return información del viaje finalizado
     */
    TripResponseDTO endTrip(TripEndRequestDTO request);

    /**
     * Obtiene la lista de viajes de un usuario, ordenados por fecha de inicio descendente.
     *
     * @param userId identificador del usuario
     * @return lista de viajes del usuario
     */
    List<TripResponseDTO> getUserTrips(Long userId);

    /**
     * Obtiene el detalle completo de un viaje específico.
     *
     * @param tripId identificador del viaje
     * @return detalle del viaje
     */
    TripDetailDTO getTripDetail(Long tripId);

    /**
     * Retorna todos los viajes actualmente en progreso.
     *
     * @return lista de viajes activos
     */
    List<TripResponseDTO> getActiveTrips();
}
