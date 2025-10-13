package co.edu.unbosque.userservice.service;

import co.edu.unbosque.userservice.dto.*;

public interface WalletService {
    void registerCard(PaymentCardRequestDTO request);
    WalletRechargeResponseDTO rechargeWallet(WalletRechargeRequestDTO request);
    void updateWalletBalance(WalletNotificationDTO notification);
    WalletDTO getWalletByUserId(Long userId);
}