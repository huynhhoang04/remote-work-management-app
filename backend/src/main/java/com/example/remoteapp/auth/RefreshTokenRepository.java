package com.example.remoteapp.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository cho {@link RefreshToken}.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Tìm refresh token theo hash (token gốc chỉ có ở client).
     *
     * @param tokenHash hash SHA-256 của token
     * @return token nếu tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Thu hồi toàn bộ refresh token của một người dùng
     * (logout / đổi mật khẩu / đặt lại mật khẩu).
     *
     * @param userId chủ sở hữu token
     * @return số bản ghi được cập nhật
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.userId = :userId and t.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId);
}
