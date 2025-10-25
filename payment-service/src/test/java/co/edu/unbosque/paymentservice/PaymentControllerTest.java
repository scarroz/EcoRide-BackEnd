package co.edu.unbosque.paymentservice;

import co.edu.unbosque.paymentservice.controller.PaymentController;
import co.edu.unbosque.paymentservice.dto.*;
import co.edu.unbosque.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Integration Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentCardRequestDTO cardRequest;
    private PaymentMethodResponseDTO paymentMethodResponse;
    private WalletRechargeRequestDTO rechargeRequest;
    private TransactionResponseDTO transactionResponse;

    @BeforeEach
    void setUp() {
        cardRequest = new PaymentCardRequestDTO(
                1L,
                "Sebastian Carroz",
                "sebastian@example.com",
                "pm_card_visa"
        );

        paymentMethodResponse = new PaymentMethodResponseDTO(
                "pm_test_token",
                "visa",
                "4242",
                false
        );

        rechargeRequest = new WalletRechargeRequestDTO(
                1L,
                new BigDecimal("25.00"),
                "pm_test_token"
        );

        transactionResponse = new TransactionResponseDTO(
                1L,
                1L,
                new BigDecimal("25.00"),
                "TOP_UP",
                "CARD",
                "COMPLETED",
                "pi_test",
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/payments/cards/register - Should register card successfully")
    void testRegisterCardSuccess() throws Exception {
        // Arrange
        when(paymentService.registerCard(any())).thenReturn(paymentMethodResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/cards/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Tarjeta registrada correctamente"))
                .andExpect(jsonPath("$.paymentMethod.brand").value("visa"))
                .andExpect(jsonPath("$.paymentMethod.last4").value("4242"));

        verify(paymentService).registerCard(any());
    }

    @Test
    @DisplayName("POST /api/payments/cards/register - Should handle registration error")
    void testRegisterCardError() throws Exception {
        // Arrange
        when(paymentService.registerCard(any()))
                .thenThrow(new RuntimeException("Error con Stripe"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/cards/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se pudo registrar la tarjeta"));

        verify(paymentService).registerCard(any());
    }

    @Test
    @DisplayName("POST /api/payments/wallet/recharge - Should recharge wallet successfully")
    void testRechargeWalletSuccess() throws Exception {
        // Arrange
        when(paymentService.rechargeWallet(any())).thenReturn(transactionResponse);

        // Act & Assert
        mockMvc.perform(post("/api/payments/wallet/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rechargeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recarga procesada correctamente"))
                .andExpect(jsonPath("$.transaction.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transaction.amount").value(25.00));

        verify(paymentService).rechargeWallet(any());
    }

    @Test
    @DisplayName("POST /api/payments/wallet/recharge - Should handle payment error")
    void testRechargeWalletPaymentError() throws Exception {
        // Arrange
        when(paymentService.rechargeWallet(any()))
                .thenThrow(new RuntimeException("Tarjeta rechazada"));

        // Act & Assert
        mockMvc.perform(post("/api/payments/wallet/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rechargeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Error procesando la recarga"));

        verify(paymentService).rechargeWallet(any());
    }

    @Test
    @DisplayName("GET /api/payments/methods/{userId} - Should get payment methods")
    void testGetUserPaymentMethodsSuccess() throws Exception {
        // Arrange
        List<PaymentMethodResponseDTO> methods = Arrays.asList(paymentMethodResponse);
        when(paymentService.getActiveMethodsByUser(anyLong())).thenReturn(methods);

        // Act & Assert
        mockMvc.perform(get("/api/payments/methods/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Métodos de pago obtenidos exitosamente"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].brand").value("visa"));

        verify(paymentService).getActiveMethodsByUser(1L);
    }

    @Test
    @DisplayName("GET /api/payments/methods/{userId} - Should handle no methods found")
    void testGetUserPaymentMethodsEmpty() throws Exception {
        // Arrange
        when(paymentService.getActiveMethodsByUser(anyLong())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/payments/methods/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(paymentService).getActiveMethodsByUser(999L);
    }

    @Test
    @DisplayName("POST /api/payments/wallet/recharge - Should validate request body")
    void testRechargeWalletInvalidRequest() throws Exception {
        // Arrange
        String invalidJson = "{\"userId\": 1}"; // Missing required fields

        // Act & Assert
        mockMvc.perform(post("/api/payments/wallet/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/payments/cards/register - Should validate empty email")
    void testRegisterCardEmptyEmail() throws Exception {
        // Arrange
        PaymentCardRequestDTO invalidRequest = new PaymentCardRequestDTO(
                1L,
                "Sebastian Carroz",
                "",
                "pm_card_visa"
        );

        // Act & Assert
        mockMvc.perform(post("/api/payments/cards/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}