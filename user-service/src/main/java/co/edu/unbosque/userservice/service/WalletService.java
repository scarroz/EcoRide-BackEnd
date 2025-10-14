package co.edu.unbosque.userservice.service;

import co.edu.unbosque.userservice.dto.*;

/**
 * Servicio para gestion de wallets y recargas
 */
public interface WalletService {

    /**
     * Registra una tarjeta en Payment-Service
     */
    void registerCard(PaymentCardRequestDTO request);

    /**
     * Procesa una recarga de wallet
     */
    WalletRechargeResponseDTO rechargeWallet(WalletRechargeRequestDTO request);

    /**
     * Actualiza el saldo de wallet cuando Payment-Service notifica
     */
    void updateWalletBalance(WalletNotificationDTO notification);

    /**
     * Obtiene la wallet de un usuario
     */
    WalletDTO getWalletByUserId(Long userId);
}