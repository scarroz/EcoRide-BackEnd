package co.edu.unbosque.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationRequestDTO(
        @NotNull(message = "userId es obligatorio")
        Integer userId,

        @NotBlank(message = "templateCode es obligatorio")
        String templateCode,

        // Opcional: canal, por defecto "EMAIL"
        String channel
) {}
