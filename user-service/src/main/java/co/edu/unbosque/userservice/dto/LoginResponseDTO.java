package co.edu.unbosque.userservice.dto;

public record LoginResponseDTO(
        String token,
        String message,
        Long userId,
        String email,
        String fullName
) {}
