package co.edu.unbosque.userservice.repository;

import co.edu.unbosque.userservice.model.PasswordRecoveryToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {

    // Buscar código válido (no expirado)
    @Query("SELECT t FROM PasswordRecoveryToken t WHERE t.code = :code AND t.expiresAt > :now")
    Optional<PasswordRecoveryToken> findValidCode(String code, LocalDateTime now);

    // Eliminar todos los códigos de un usuario
    @Modifying
    @Query("DELETE FROM PasswordRecoveryToken t WHERE t.userId = :userId")
    void deleteByUserId(Long userId);
}