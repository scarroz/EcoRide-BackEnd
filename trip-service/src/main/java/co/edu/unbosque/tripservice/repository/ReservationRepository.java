package co.edu.unbosque.tripservice.repository;

import co.edu.unbosque.tripservice.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByUserIdAndStatus(Long userId, String status);

    List<Reservation> findByBicycleIdAndStatus(Long bicycleId, String status);

    @Query("SELECT r FROM Reservation r WHERE r.status = 'ACTIVE' AND r.expiresAt < :now")
    List<Reservation> findExpiredReservations(LocalDateTime now);

    List<Reservation> findByUserId(Long userId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.userId = :userId AND r.status = 'ACTIVE'")
    Integer countActiveReservationsByUser(Long userId);
}
