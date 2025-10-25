package co.edu.unbosque.notificationservice.repository;

import co.edu.unbosque.notificationservice.model.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Integer> {

    // En NotificationLogRepository
    Optional<NotificationLog> findTopByUserIdOrderByIdDesc(Integer userId);
}
