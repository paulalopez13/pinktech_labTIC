package uy.edu.um.proyTic2026.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import uy.edu.um.proyTic2026.entities.PasswordResetToken;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
    void deleteAllByUser_Id(Long userId);
    void deleteAllByExpiresAtBefore(Instant cutoff);
}
