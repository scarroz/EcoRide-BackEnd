package co.edu.unbosque.tripservice.repository;


import co.edu.unbosque.tripservice.model.Bicycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BicycleRepository extends JpaRepository<Bicycle, Long> {

    Optional<Bicycle> findByCode(String code);

    List<Bicycle> findByLastStationIdAndStatus(Long stationId, String status);

    @Query("SELECT COUNT(b) FROM Bicycle b WHERE b.lastStation.id = :stationId AND b.status = 'AVAILABLE'")
    Integer countAvailableBicyclesByStation(Long stationId);

    @Query("SELECT b FROM Bicycle b WHERE b.lastStation.id = :stationId AND b.status = 'AVAILABLE' " +
            "AND (b.type = 'MECHANICAL' OR (b.type = 'ELECTRIC' AND b.batteryLevel >= 40))")
    List<Bicycle> findAvailableBicyclesByStation(Long stationId);

    List<Bicycle> findByStatus(String status);

    @Query("SELECT b FROM Bicycle b WHERE b.type = 'ELECTRIC' AND b.batteryLevel < 40")
    List<Bicycle> findLowBatteryBicycles();
}

