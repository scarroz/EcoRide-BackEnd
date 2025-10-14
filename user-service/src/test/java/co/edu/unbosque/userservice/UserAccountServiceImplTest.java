package co.edu.unbosque.userservice;


import co.edu.unbosque.userservice.dto.*;
import co.edu.unbosque.userservice.exception.ResourceNotFoundException;
import co.edu.unbosque.userservice.mapper.DataMapper;
import co.edu.unbosque.userservice.model.UserAccount;
import co.edu.unbosque.userservice.model.UserProfile;
import co.edu.unbosque.userservice.model.Wallet;
import co.edu.unbosque.userservice.repository.UserAccountRepository;
import co.edu.unbosque.userservice.repository.UserProfileRepository;
import co.edu.unbosque.userservice.repository.WalletRepository;
import co.edu.unbosque.userservice.service.impl.UserAccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserAccountServiceImplTest {

    @Mock private UserAccountRepository userRepo;
    @Mock private WalletRepository walletRepo;
    @Mock private UserProfileRepository profileRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private DataMapper mapper;

    @InjectMocks
    private UserAccountServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_ShouldCreateUserAndWallet() {
        UserAccountRequestDTO request = new UserAccountRequestDTO("Juan Pérez", "123456789", "juan@example.com", "pass123");

        // mocks
        when(userRepo.findByEmail("juan@example.com")).thenReturn(Optional.empty());
        when(userRepo.findByDocumentNumber("123456789")).thenReturn(Optional.empty());
        when(encoder.encode("pass123")).thenReturn("encodedPass");

        UserAccount savedUser = new UserAccount();
        savedUser.setId(1L);
        when(mapper.toUserAccountEntity(request, "encodedPass")).thenReturn(savedUser);
        when(userRepo.save(any(UserAccount.class))).thenReturn(savedUser);

        Wallet walletEntity = new Wallet();
        walletEntity.setId(1L);
        walletEntity.setBalance(BigDecimal.ZERO);
        when(mapper.createInitialWallet(savedUser)).thenReturn(walletEntity);
        when(walletRepo.save(walletEntity)).thenReturn(walletEntity);

        UserAccountResponseDTO responseDTO = new UserAccountResponseDTO(
                1L, "Juan Pérez", "juan@example.com", "123456789",
                "ACTIVE", false, BigDecimal.ZERO, null
        );
        when(mapper.toUserAccountResponseDTO(savedUser, walletEntity)).thenReturn(responseDTO);

        // ejecución
        UserAccountResponseDTO response = userService.registerUser(request);

        // verificaciones
        assertNotNull(response);
        assertEquals("Juan Pérez", response.fullName());
        assertEquals(BigDecimal.ZERO, response.walletBalance());
        verify(userRepo, times(1)).save(any(UserAccount.class));
        verify(walletRepo, times(1)).save(walletEntity);
    }
    @Test
    void registerUser_ShouldThrowIfEmailExists() {
        when(userRepo.findByEmail("test@example.com")).thenReturn(Optional.of(new UserAccount()));
        UserAccountRequestDTO request = new UserAccountRequestDTO("Test", "999", "test@example.com", "1234");
        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.TEN);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(walletRepo.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(mapper.toUserAccountResponseDTO(user, wallet))
                .thenReturn(new UserAccountResponseDTO(1L, "Test", "a@b.com", "123", "ACTIVE", false, BigDecimal.TEN, null));

        var result = userService.getUserById(1L);

        assertEquals(BigDecimal.TEN, result.walletBalance());
    }

    @Test
    void getUserById_ShouldThrowIfNotFound() {
        when(userRepo.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(10L));
    }

    @Test
    void createOrUpdateProfile_ShouldCreateNewProfile() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        UserProfile profile = new UserProfile();

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(mapper.toUserProfileEntity(any(), eq(user))).thenReturn(profile);
        when(profileRepo.save(profile)).thenReturn(profile);
        when(mapper.toUserProfileDTO(profile)).thenReturn(new UserProfileDTO(1L, "+57 300 1234567", "Calle 123", "Bogotá", "Colombia"));

        UserProfileDTO dto = userService.createOrUpdateProfile(1L, new UserProfileRequestDTO(99L,"+57 300 1234567", "Calle 123", "Bogotá", "Colombia"));

        assertNotNull(dto);
        assertEquals("Bogotá", dto.city());
    }
}
