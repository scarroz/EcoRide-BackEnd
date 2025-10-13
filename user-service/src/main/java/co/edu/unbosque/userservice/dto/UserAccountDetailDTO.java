package co.edu.unbosque.userservice.dto;

import java.time.LocalDateTime;

public record UserAccountDetailDTO(Long id,
                                   String fullName,
                                   String email,
                                   String documentNumber,
                                   String status,
                                   boolean verified,
                                   LocalDateTime createdAt,
                                   UserProfileDTO profile,
                                   WalletDTO wallet) {
}
