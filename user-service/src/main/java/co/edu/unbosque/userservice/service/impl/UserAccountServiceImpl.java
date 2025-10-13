package co.edu.unbosque.userservice.service.impl;

import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.exception.ResourceNotFoundException;
import co.edu.unbosque.userservice.mapper.DataMapper;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.model.UserProfile;
import co.edu.unbosque.userservice.model.Wallet;
import co.edu.unbosque.userservice.repository.UserAccountRepository;
import co.edu.unbosque.userservice.repository.UserProfileRepository;
import co.edu.unbosque.userservice.repository.WalletRepository;
import co.edu.unbosque.userservice.service.UserAccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountRepository userRepo;
    private final WalletRepository walletRepo;
    private final UserProfileRepository profileRepo;
    private final PasswordEncoder encoder;
    private final DataMapper mapper;

    public UserAccountServiceImpl(
            UserAccountRepository userRepo,
            WalletRepository walletRepo,
            UserProfileRepository profileRepo,
            PasswordEncoder encoder,
            DataMapper mapper
    ) {
        this.userRepo = userRepo;
        this.walletRepo = walletRepo;
        this.profileRepo = profileRepo;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public UserAccountResponseDTO registerUser(UserAccountRequestDTO request) {
        System.out.println("Registrando nuevo usuario: " + request.email());

        // Validar que el email no exista
        if (userRepo.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("El email ya esta registrado");
        }

        // Validar que el documento no exista
        if (userRepo.findByDocumentNumber(request.documentNumber()).isPresent()) {
            throw new IllegalArgumentException("El documento ya esta registrado");
        }

        // Crear usuario usando mapper
        String encodedPassword = encoder.encode(request.password());
        UserAccount user = mapper.toUserAccountEntity(request, encodedPassword);
        user = userRepo.save(user);

        System.out.println("Usuario creado con ID: " + user.getId());

        // Crear wallet inicial usando mapper
        Wallet wallet = mapper.createInitialWallet(user);
        wallet = walletRepo.save(wallet);

        System.out.println("Wallet creada con saldo inicial: " + wallet.getBalance());

        // Retornar DTO usando mapper
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
}