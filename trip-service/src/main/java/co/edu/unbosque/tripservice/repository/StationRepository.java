package co.edu.unbosque.tripservice.repository;


import co.edu.unbosque.tripservice.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByActiveTrue();

    Optional<Station> findByName(String name);

    @Query("SELECT s FROM Station s WHERE s.active = true AND " +
            "SQRT(POWER(s.latitude - :lat, 2) + POWER(s.longitude - :lon, 2)) < :radius")
    List<Station> findNearbyStations(java.math.BigDecimal lat, java.math.BigDecimal lon, double radius);
}

