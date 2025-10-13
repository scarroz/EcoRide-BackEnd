package co.edu.unbosque.userservice.dto;

public record UserAccountRequestDTO(
        String fullName,
        String documentNumber,
        String email,
        String password
) {}
