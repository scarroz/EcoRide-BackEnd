package co.edu.unbosque.notificationservice.repository;

import co.edu.unbosque.notificationservice.model.MessageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Integer> {
    Optional<MessageTemplate> findByCode(String code);
}
