package com.example.remoteapp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository cho {@link PasswordResetToken}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Tìm token đặt lại mật khẩu theo hash.
     *
     * @param tokenHash hash SHA-256 của token trong link email
     * @return token nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
