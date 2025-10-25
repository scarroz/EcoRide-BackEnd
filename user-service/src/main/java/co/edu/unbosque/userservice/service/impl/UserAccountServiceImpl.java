package co.edu.unbosque.userservice.service.impl;

import co.edu.unbosque.userservice.client.NotificationClient;
import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.exception.ResourceNotFoundException;
import co.edu.unbosque.userservice.mapper.DataMapper;
import co.edu.unbosque.userservice.model.PasswordRecoveryToken;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.model.UserProfile;
import co.edu.unbosque.userservice.model.Wallet;
import co.edu.unbosque.userservice.repository.PasswordRecoveryTokenRepository;
import co.edu.unbosque.userservice.repository.UserAccountRepository;
import co.edu.unbosque.userservice.repository.UserProfileRepository;
import co.edu.unbosque.userservice.repository.WalletRepository;
import co.edu.unbosque.userservice.service.UserAccountService;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class UserAccountServiceImpl implements UserAccountService {


    private final UserAccountRepository userRepo;
    private final WalletRepository walletRepo;
    private final UserProfileRepository profileRepo;
    private final PasswordEncoder encoder;
    private final DataMapper mapper;
    private final NotificationClient notificationClient;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordRecoveryTokenRepository tokenRepository;

    public UserAccountServiceImpl(
            UserAccountRepository userRepo,
            PasswordRecoveryTokenRepository tokenRepository,
            WalletRepository walletRepo,
            UserProfileRepository profileRepo,
            PasswordEncoder encoder,
            DataMapper mapper,
            NotificationClient notificationClient,
            ApplicationEventPublisher
                    eventPublisher
    ) {
        this.userRepo = userRepo;
        this.walletRepo = walletRepo;
        this.tokenRepository = tokenRepository;
        this.profileRepo = profileRepo;
        this.encoder = encoder;
        this.mapper = mapper;
        this.notificationClient = notificationClient;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserAccountResponseDTO registerUser(UserAccountRequestDTO request) {
        System.out.println("Registrando nuevo usuario: " + request.email());

        if (userRepo.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        if (userRepo.findByDocumentNumber(request.documentNumber()).isPresent()) {
            throw new IllegalArgumentException("El documento ya está registrado");
        }

        String encodedPassword = encoder.encode(request.password());
        UserAccount user = mapper.toUserAccountEntity(request, encodedPassword);
        user = userRepo.save(user);

        Wallet wallet = mapper.createInitialWallet(user);
        wallet = walletRepo.save(wallet);

        // Publicamos evento para notificación después del commit
        eventPublisher.publishEvent(user);

        return mapper.toUserAccountResponseDTO(user, wallet);
    }



    @Override
    public List<UserAccountResponseDTO> listAllUsers() {
        System.out.println("Listando todos los usuarios");

        return userRepo.findAll().stream()
                .map(user -> {
                    Wallet wallet = walletRepo.findByUserId(user.getId()).orElse(null);
                    return mapper.toUserAccountResponseDTO(user, wallet);
                })
                .collect(Collectors.toList());
    }

    @Override
    public UserAccountResponseDTO getUserById(Long id) {
        System.out.println("Obteniendo usuario con ID: " + id);

        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Wallet wallet = walletRepo.findByUserId(id).orElse(null);

        return mapper.toUserAccountResponseDTO(user, wallet);
    }

    @Override
    public UserAccountDetailDTO getUserDetailById(Long id) {
        System.out.println("Obteniendo detalle completo del usuario con ID: " + id);

        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserProfile profile = profileRepo.findByUserId(id).orElse(null);
        Wallet wallet = walletRepo.findByUserId(id).orElse(null);

        return mapper.toUserAccountDetailDTO(user, profile, wallet);
    }

    @Override
    @Transactional
    public UserAccountResponseDTO updateUser(Long id, UserAccountUpdateDTO request) {
        System.out.println("Actualizando usuario con ID: " + id);

        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Si se envia password, encriptarlo
        String encodedPassword = null;
        if (request.password() != null && !request.password().isBlank()) {
            encodedPassword = encoder.encode(request.password());
        }

        // Actualizar usando mapper
        mapper.updateUserAccountFromDTO(user, request, encodedPassword);
        user = userRepo.save(user);

        System.out.println("Usuario actualizado: " + user.getId());

        Wallet wallet = walletRepo.findByUserId(id).orElse(null);
        return mapper.toUserAccountResponseDTO(user, wallet);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        System.out.println("Eliminando usuario con ID: " + id);

        if (!userRepo.existsById(id)) {
            throw new ResourceNotFoundException("Usuario no encontrado");
        }

        userRepo.deleteById(id);
        System.out.println("Usuario eliminado exitosamente");
    }

    @Override
    @Transactional
    public UserProfileDTO createOrUpdateProfile(Long userId, UserProfileRequestDTO request) {
        System.out.println("Creando/actualizando perfil para usuario: " + userId);

        UserAccount user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserProfile profile = profileRepo.findByUserId(userId)
                .orElse(null);

        if (profile == null) {
            // Crear nuevo perfil
            profile = mapper.toUserProfileEntity(request, user);
            System.out.println("Creando nuevo perfil");
        } else {
            // Actualizar perfil existente
            mapper.updateUserProfileFromDTO(profile, request);
            System.out.println("Actualizando perfil existente");
        }

        profile = profileRepo.save(profile);
        return mapper.toUserProfileDTO(profile);
    }

    @Override
    public UserProfileDTO getUserProfile(Long userId) {
        System.out.println("Obteniendo perfil de usuario: " + userId);

        UserProfile profile = profileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        return mapper.toUserProfileDTO(profile);
    }

    @Transactional
    public String requestPasswordRecovery(Long userId) {
        System.out.println("Solicitando recuperación de contraseña para userId: " + userId);

        // Buscar usuario
        UserAccount user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("No existe usuario con id: " + userId));

        // Generar código de 6 dígitos
        String code = generateCode();

        // Eliminar códigos anteriores
        tokenRepository.deleteByUserId(user.getId());

        // Crear y GUARDAR token en la BD
        PasswordRecoveryToken token = new PasswordRecoveryToken(user.getId(), code, 15);
        tokenRepository.save(token); //  ESTO YA ESTÁ BIEN

        // Enviar código por email
        try {
            notificationClient.sendPasswordRecoveryCode(user.getId().intValue(), code);
            System.out.println("Código enviado exitosamente al notification-service");
        } catch (Exception e) {
            System.err.println("Error al comunicarse con notification-service: " + e.getMessage());
            throw new RuntimeException("Error al enviar el código de recuperación");
        }

        return "Código enviado a " + maskEmail(user.getEmail());
    }

    @Transactional
    public String resetPassword(String email, String code, String newPassword) {
        System.out.println("Restableciendo contraseña para: " + email);

        UserAccount user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No existe usuario con ese email"));

        PasswordRecoveryToken token = tokenRepository.findValidCode(code, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Código inválido o expirado"));

        if (!token.getUserId().equals(user.getId())) {
            throw new RuntimeException("Código inválido para este usuario");
        }

        user.setPasswordHash(encoder.encode(newPassword));
        userRepo.save(user);
        tokenRepository.delete(token);

        System.out.println("Contraseña cambiada exitosamente para: " + email);
        return "Contraseña cambiada exitosamente";
    }


    // ==================== MÉTODOS AUXILIARES ====================

    private String generateCode() {
        return String.valueOf(100000 + new SecureRandom().nextInt(900000));
    }

    private String maskEmail(String email) {
        if (!email.contains("@")) return email;
        String[] parts = email.split("@");
        String local = parts[0];
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }

}