package co.edu.unbosque.userservice.service.impl;

import co.edu.unbosque.userservice.config.JwtTokenUtil;
import co.edu.unbosque.userservice.dto.LoginRequestDTO;
import co.edu.unbosque.userservice.dto.LoginResponseDTO;
import co.edu.unbosque.userservice.exception.ResourceNotFoundException;
import co.edu.unbosque.userservice.mapper.DataMapper;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.repository.UserAccountRepository;
import co.edu.unbosque.userservice.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementacion del servicio de autenticacion
 * Maneja login y generacion de tokens JWT
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final DataMapper mapper;

    public AuthServiceImpl(
            UserAccountRepository userAccountRepository,
            JwtTokenUtil jwtTokenUtil,
            PasswordEncoder passwordEncoder,
            DataMapper mapper
    ) {
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        System.out.println("Intento de login para: " + request.email());

        // Buscar usuario por email
        UserAccount user = userAccountRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con email: " + request.email()
                ));

        // Validar password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            System.out.println("Credenciales invalidas para: " + request.email());
            throw new IllegalArgumentException("Credenciales invalidas");
        }

        // Validar que el usuario este activo
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("La cuenta no esta activa");
        }

        System.out.println("Login exitoso para: " + user.getEmail());

        // Generar token JWT
        String token = jwtTokenUtil.generateToken(user.getEmail());

        // Retornar DTO usando mapper con informacion completa
        return mapper.toLoginResponseDTO(token, user);
    }
}