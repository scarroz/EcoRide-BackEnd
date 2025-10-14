package co.edu.unbosque.userservice.mapper;

import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.model.UserProfile;
import co.edu.unbosque.userservice.model.Wallet;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapper para convertir entre DTOs y Entidades
 * Centraliza toda la logica de mapeo del User Service
 */
@Component
public class DataMapper {

    // ========================================
    // USER ACCOUNT MAPPINGS
    // ========================================

    /**
     * Crea entidad UserAccount desde request DTO
     */
    public UserAccount toUserAccountEntity(UserAccountRequestDTO dto, String encodedPassword) {
        if (dto == null) return null;

        UserAccount user = new UserAccount();
        user.setFullName(dto.fullName());
        user.setDocumentNumber(dto.documentNumber());
        user.setEmail(dto.email());
        user.setPasswordHash(encodedPassword);
        user.setVerified(false);
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        return user;
    }

    /**
     * Convierte UserAccount a DTO de respuesta basica
     */
    public UserAccountResponseDTO toUserAccountResponseDTO(UserAccount entity, Wallet wallet) {
        if (entity == null) return null;

        BigDecimal balance = wallet != null ? wallet.getBalance() : BigDecimal.ZERO;

        return new UserAccountResponseDTO(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getDocumentNumber(),
                entity.getStatus(),
                entity.isVerified(),
                balance,
                entity.getCreatedAt()
        );
    }

    /**
     * Convierte UserAccount a DTO detallado
     */
    public UserAccountDetailDTO toUserAccountDetailDTO(
            UserAccount entity,
            UserProfile profile,
            Wallet wallet
    ) {
        if (entity == null) return null;

        return new UserAccountDetailDTO(
                entity.getId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getDocumentNumber(),
                entity.getStatus(),
                entity.isVerified(),
                entity.getCreatedAt(),
                toUserProfileDTO(profile),
                toWalletDTO(wallet)
        );
    }

    /**
     * Actualiza entidad UserAccount desde DTO de actualizacion
     */
    public void updateUserAccountFromDTO(UserAccount entity, UserAccountUpdateDTO dto, String encodedPassword) {
        if (entity == null || dto == null) return;

        if (dto.fullName() != null && !dto.fullName().isBlank()) {
            entity.setFullName(dto.fullName());
        }
        if (dto.email() != null && !dto.email().isBlank()) {
            entity.setEmail(dto.email());
        }
        if (dto.password() != null && !dto.password().isBlank() && encodedPassword != null) {
            entity.setPasswordHash(encodedPassword);
        }
    }

    // ========================================
    // USER PROFILE MAPPINGS
    // ========================================

    /**
     * Crea entidad UserProfile desde request DTO
     */
    public UserProfile toUserProfileEntity(UserProfileRequestDTO dto, UserAccount user) {
        if (dto == null || user == null) return null;

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setPhone(dto.phone());
        profile.setAddress(dto.address());
        profile.setCity(dto.city());
        profile.setCountry(dto.country());

        return profile;
    }

    /**
     * Convierte UserProfile a DTO
     */
    public UserProfileDTO toUserProfileDTO(UserProfile entity) {
        if (entity == null) {
            return new UserProfileDTO(null, null, null, null, null);
        }

        return new UserProfileDTO(
                entity.getId(),
                entity.getPhone(),
                entity.getAddress(),
                entity.getCity(),
                entity.getCountry()
        );
    }

    /**
     * Actualiza entidad UserProfile desde DTO
     */
    public void updateUserProfileFromDTO(UserProfile entity, UserProfileRequestDTO dto) {
        if (entity == null || dto == null) return;

        if (dto.phone() != null) {
            entity.setPhone(dto.phone());
        }
        if (dto.address() != null) {
            entity.setAddress(dto.address());
        }
        if (dto.city() != null) {
            entity.setCity(dto.city());
        }
        if (dto.country() != null) {
            entity.setCountry(dto.country());
        }
    }

    // ========================================
    // WALLET MAPPINGS
    // ========================================

    /**
     * Crea entidad Wallet inicial para nuevo usuario
     */
    public Wallet createInitialWallet(UserAccount user) {
        if (user == null) return null;

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setLastUpdated(LocalDateTime.now());

        return wallet;
    }

    /**
     * Convierte Wallet a DTO
     */
    public WalletDTO toWalletDTO(Wallet entity) {
        if (entity == null) return null;

        return new WalletDTO(
                entity.getId(),
                entity.getUser().getId(),
                entity.getBalance(),
                entity.getLastUpdated()
        );
    }

    /**
     * Crea DTO de respuesta de recarga
     */
    public WalletRechargeResponseDTO toWalletRechargeResponseDTO(
            Wallet wallet,
            BigDecimal rechargeAmount,
            String transactionId
    ) {
        if (wallet == null) return null;

        return new WalletRechargeResponseDTO(
                "Recarga procesada exitosamente",
                wallet.getBalance(),
                rechargeAmount,
                transactionId
        );
    }

    // ========================================
    // AUTHENTICATION MAPPINGS
    // ========================================

    /**
     * Crea DTO de respuesta de login con informacion completa
     */
    public LoginResponseDTO toLoginResponseDTO(String token, UserAccount user) {
        if (user == null) return null;

        return new LoginResponseDTO(
                token,
                "Login exitoso",
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }

    // ========================================
    // NOTIFICATION MAPPINGS
    // ========================================

    /**
     * Crea DTO de notificacion de wallet
     */
    public WalletNotificationDTO toWalletNotificationDTO(Long userId, BigDecimal amount) {
        return new WalletNotificationDTO(userId, amount);
    }

    /**
     * Crea DTO para registrar tarjeta en Payment-Service
     */
    public PaymentCardRequestDTO toPaymentCardRequestDTO(
            Long userId,
            String name,
            String email,
            String paymentMethodId
    ) {
        return new PaymentCardRequestDTO(userId, name, email, paymentMethodId);
    }
}