package co.edu.unbosque.userservice.dto;

public record UserAccountUpdateDTO(
        String fullName,
        String email,
        String password
) {}
