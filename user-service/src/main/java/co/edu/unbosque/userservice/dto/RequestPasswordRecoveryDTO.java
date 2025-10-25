package co.edu.unbosque.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestPasswordRecoveryDTO(
        @NotBlank @Email String email
) {}
