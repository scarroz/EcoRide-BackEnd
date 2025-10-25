package co.edu.unbosque.paymentservice;

import co.edu.unbosque.paymentservice.client.UserNotificationClient;
import co.edu.unbosque.paymentservice.dto.*;
import co.edu.unbosque.paymentservice.mapper.DataMapper;
import co.edu.unbosque.paymentservice.model.PaymentMethod;
import co.edu.unbosque.paymentservice.model.Transaction;
import co.edu.unbosque.paymentservice.repository.PaymentMethodRepository;
import co.edu.unbosque.paymentservice.repository.TransactionRepository;
import co.edu.unbosque.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Tests")
class PaymentServiceImplTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserNotificationClient userNotificationClient;

    @Mock
    private DataMapper mapper;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentCardRequestDTO cardRequest;
    private PaymentMethod paymentMethod;
    private WalletRechargeRequestDTO rechargeRequest;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_dummy");

        cardRequest = new PaymentCardRequestDTO(
                1L,
                "Sebastian Carroz",
                "sebastian@example.com",
                "pm_card_visa"
        );

        paymentMethod = new PaymentMethod();
        paymentMethod.setId(1L);
        paymentMethod.setUserId(1L);
        paymentMethod.setTokenId("pm_test_token");
        paymentMethod.setStripeCustomerId("cus_test");
        paymentMethod.setCardBrand("visa");
        paymentMethod.setCardLast4("4242");
        paymentMethod.setActive(true);

        rechargeRequest = new WalletRechargeRequestDTO(
                1L,
                new BigDecimal("25.00"),
                "pm_test_token"
        );

        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setUserId(1L);
        transaction.setAmount(new BigDecimal("25.00"));
        transaction.setType("TOP_UP");
        transaction.setSource("CARD");
        transaction.setStatus("PENDING");
    }

    @Test
    @DisplayName("Should get active payment methods by user")
    void testGetActiveMethodsByUser() {
        // Arrange
        Long userId = 1L;
        List<PaymentMethod> methods = Arrays.asList(paymentMethod);
        PaymentMethodResponseDTO responseDTO = new PaymentMethodResponseDTO(
                "pm_test_token",
                "visa",
                "4242",
                false
        );

        when(paymentMethodRepository.findByUserIdAndActiveTrue(userId)).thenReturn(methods);
        when(mapper.toPaymentMethodResponseDTO(any())).thenReturn(responseDTO);

        // Act
        List<PaymentMethodResponseDTO> result = paymentService.getActiveMethodsByUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("visa", result.get(0).brand());
        assertEquals("4242", result.get(0).last4());
        verify(paymentMethodRepository).findByUserIdAndActiveTrue(userId);
        verify(mapper).toPaymentMethodResponseDTO(any());
    }

    @Test
    @DisplayName("Should return empty list when user has no payment methods")
    void testGetActiveMethodsByUserNoMethods() {
        // Arrange
        Long userId = 999L;
        when(paymentMethodRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        // Act
        List<PaymentMethodResponseDTO> result = paymentService.getActiveMethodsByUser(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paymentMethodRepository).findByUserIdAndActiveTrue(userId);
    }

    @Test
    @DisplayName("Should get transactions by user")
    void testGetTransactionsByUser() {
        // Arrange
        Long userId = 1L;
        List<Transaction> transactions = Arrays.asList(transaction);
        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                1L,
                1L,
                new BigDecimal("25.00"),
                "TOP_UP",
                "CARD",
                "COMPLETED",
                "pi_test",
                transaction.getCreatedAt()
        );

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(transactions);
        when(mapper.toTransactionResponseDTO(any())).thenReturn(responseDTO);

        // Act
        List<TransactionResponseDTO> result = paymentService.getTransactionsByUser(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TOP_UP", result.get(0).type());
        assertEquals("COMPLETED", result.get(0).status());
        verify(transactionRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    @DisplayName("Should get transaction detail with payment method info")
    void testGetTransactionDetail() {
        // Arrange
        Long transactionId = 1L;
        transaction.setPaymentMethodId(1L);

        TransactionDetailDTO detailDTO = new TransactionDetailDTO(
                1L, 1L, null, 1L, null,
                new BigDecimal("25.00"),
                "TOP_UP", "CARD", "COMPLETED",
                "pi_test", transaction.getCreatedAt(),
                "visa", "4242"
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(paymentMethod));
        when(mapper.toTransactionDetailDTO(transaction, paymentMethod)).thenReturn(detailDTO);

        // Act
        TransactionDetailDTO result = paymentService.getTransactionDetail(transactionId);

        // Assert
        assertNotNull(result);
        assertEquals("visa", result.cardBrand());
        assertEquals("4242", result.cardLast4());
        verify(transactionRepository).findById(transactionId);
        verify(paymentMethodRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when transaction not found")
    void testGetTransactionDetailNotFound() {
        // Arrange
        Long transactionId = 999L;
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                paymentService.getTransactionDetail(transactionId)
        );
        verify(transactionRepository).findById(transactionId);
    }

    @Test
    @DisplayName("Should get transactions by user and status")
    void testGetTransactionsByUserAndStatus() {
        // Arrange
        Long userId = 1L;
        String status = "COMPLETED";
        transaction.setStatus(status);
        List<Transaction> transactions = Arrays.asList(transaction);

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                1L, 1L, new BigDecimal("25.00"),
                "TOP_UP", "CARD", status,
                "pi_test", transaction.getCreatedAt()
        );

        when(transactionRepository.findByUserIdAndStatus(userId, status)).thenReturn(transactions);
        when(mapper.toTransactionResponseDTO(any())).thenReturn(responseDTO);

        // Act
        List<TransactionResponseDTO> result = paymentService.getTransactionsByUserAndStatus(userId, status);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(status, result.get(0).status());
        verify(transactionRepository).findByUserIdAndStatus(userId, status);
    }

    @Test
    @DisplayName("Should validate payment method belongs to user")
    void testRechargeWalletPaymentMethodNotBelongsToUser() {
        // Arrange
        paymentMethod.setUserId(2L); // Different user
        when(paymentMethodRepository.findByTokenId(anyString()))
                .thenReturn(Optional.of(paymentMethod));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                paymentService.rechargeWallet(rechargeRequest)
        );
        verify(paymentMethodRepository).findByTokenId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when payment method is inactive")
    void testRechargeWalletInactivePaymentMethod() {
        // Arrange
        paymentMethod.setActive(false);
        when(paymentMethodRepository.findByTokenId(anyString()))
                .thenReturn(Optional.of(paymentMethod));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                paymentService.rechargeWallet(rechargeRequest)
        );
        verify(paymentMethodRepository).findByTokenId(anyString());
    }

    @Test
    @DisplayName("Should throw exception when payment method not found")
    void testRechargeWalletPaymentMethodNotFound() {
        // Arrange
        when(paymentMethodRepository.findByTokenId(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                paymentService.rechargeWallet(rechargeRequest)
        );
        verify(paymentMethodRepository).findByTokenId(anyString());
    }

    @Test
    @DisplayName("Should handle null or empty payment method ID")
    void testRechargeWalletNullPaymentMethodId() {
        // Arrange
        WalletRechargeRequestDTO invalidRequest = new WalletRechargeRequestDTO(
                1L,
                new BigDecimal("25.00"),
                null
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                paymentService.rechargeWallet(invalidRequest)
        );
    }

    @Test
    @DisplayName("Should handle blank payment method ID")
    void testRechargeWalletBlankPaymentMethodId() {
        // Arrange
        WalletRechargeRequestDTO invalidRequest = new WalletRechargeRequestDTO(
                1L,
                new BigDecimal("25.00"),
                "   "
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                paymentService.rechargeWallet(invalidRequest)
        );
    }
}