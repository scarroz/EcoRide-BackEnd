package co.edu.unbosque.userservice.service;

import co.edu.unbosque.userservice.dto.LoginRequestDTO;
import co.edu.unbosque.userservice.dto.LoginResponseDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO request);
}
