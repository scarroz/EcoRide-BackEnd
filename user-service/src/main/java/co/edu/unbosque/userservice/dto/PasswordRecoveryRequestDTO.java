package co.edu.unbosque.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Email;

public record PasswordRecoveryRequestDTO(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email inválido")
        String email
) {}
