package co.edu.unbosque.notificationservice.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Integer id,
        Integer userId,
        String templateCode,
        String status,
        LocalDateTime sentAt
) {}
