package co.edu.unbosque.userservice.repository;

import co.edu.unbosque.userservice.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByDocumentNumber(String documentNumber);

}
