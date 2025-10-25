package co.edu.unbosque.tripservice.repository;

import co.edu.unbosque.tripservice.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserId(Long userId);

    List<Trip> findByUserIdOrderByStartTimeDesc(Long userId);

    Optional<Trip> findByUserIdAndStatus(Long userId, String status);

    List<Trip> findByStatus(String status);

    @Query("SELECT t FROM Trip t WHERE t.startTime BETWEEN :start AND :end")
    List<Trip> findTripsBetweenDates(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Trip t WHERE t.status = 'COMPLETED' AND t.startTime >= :date")
    Long countCompletedTripsAfterDate(LocalDateTime date);

    @Query("SELECT SUM(t.totalCost) FROM Trip t WHERE t.status = 'COMPLETED' AND t.startTime >= :date")
    java.math.BigDecimal sumTotalRevenueAfterDate(LocalDateTime date);
}