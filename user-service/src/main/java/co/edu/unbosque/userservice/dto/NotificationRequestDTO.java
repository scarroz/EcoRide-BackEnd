package co.edu.unbosque.userservice.dto;

public record NotificationRequestDTO(
        Long userId,
        String templateCode,
        String channel,
        String extraData
) {}
