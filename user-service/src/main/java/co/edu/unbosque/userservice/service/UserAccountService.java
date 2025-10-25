package co.edu.unbosque.userservice.service;

import co.edu.unbosque.userservice.dto.*;
import java.util.List;

public interface UserAccountService {
    UserAccountResponseDTO registerUser(UserAccountRequestDTO request);
    List<UserAccountResponseDTO> listAllUsers();
    UserAccountResponseDTO getUserById(Long id);
    UserAccountDetailDTO getUserDetailById(Long id);
    UserAccountResponseDTO updateUser(Long id, UserAccountUpdateDTO request);
    void deleteUser(Long id);
    UserProfileDTO createOrUpdateProfile(Long userId, UserProfileRequestDTO request);
    UserProfileDTO getUserProfile(Long userId);
    String requestPasswordRecovery(Long id);
    String resetPassword(String email, String code, String newPassword);

}