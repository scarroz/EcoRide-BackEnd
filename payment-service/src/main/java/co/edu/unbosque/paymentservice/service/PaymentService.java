package co.edu.unbosque.paymentservice.service;

import co.edu.unbosque.paymentservice.dto.*;
import java.util.List;

public interface PaymentService {

    PaymentMethodResponseDTO registerCard(PaymentCardRequestDTO request);

    TransactionResponseDTO rechargeWallet(WalletRechargeRequestDTO request);

    List<PaymentMethodResponseDTO> getActiveMethodsByUser(Long userId);

    List<TransactionResponseDTO> getTransactionsByUser(Long userId);

    TransactionDetailDTO getTransactionDetail(Long transactionId);
    List<TransactionResponseDTO> getTransactionsByUserAndStatus(Long userId, String status);

}