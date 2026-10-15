package com.example.remoteapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link TokenService} — hash token, xoay vòng, thu hồi, reset token.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2, §A3
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private TokenService tokenService() {
        return new TokenService(refreshTokenRepository, passwordResetTokenRepository, 30, 30);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void createRefreshTokenStoresHashNotRawToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String rawToken = tokenService().createRefreshToken(UUID.randomUUID(), "device-1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(rawToken).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken).hasSize(64);
        assertThat(captor.getValue().isRevoked()).isFalse();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void rotateRevokesOldAndIssuesNew() {
        RefreshToken stored = new RefreshToken();
        stored.setUserId(UUID.randomUUID());
        stored.setTokenHash("hash");
        stored.setExpiresAt(Instant.now().plusSeconds(3600));
        stored.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TokenService.RefreshRotation rotation = tokenService().rotate("raw-token");

        assertThat(rotation.userId()).isEqualTo(stored.getUserId());
        assertThat(rotation.newRawToken()).isNotBlank().isNotEqualTo("raw-token");
        assertThat(stored.isRevoked()).isTrue();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService().rotate("unknown"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void rotateRejectsRevokedAndExpiredTokens() {
        RefreshToken revoked = new RefreshToken();
        revoked.setRevoked(true);
        revoked.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> tokenService().rotate("revoked"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_INVALID));

        RefreshToken expired = new RefreshToken();
        expired.setRevoked(false);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenService().rotate("expired"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void revokeAllForUserDelegatesToRepository() {
        UUID userId = UUID.randomUUID();

        tokenService().revokeAllForUser(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void consumeResetTokenMarksUsedAndReturnsOwner() {
        PasswordResetToken stored = new PasswordResetToken();
        stored.setUserId(UUID.randomUUID());
        stored.setTokenHash("hash");
        stored.setExpiresAt(Instant.now().plusSeconds(1800));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID userId = tokenService().consumePasswordResetToken("raw-token");

        assertThat(userId).isEqualTo(stored.getUserId());
        assertThat(stored.getUsedAt()).isNotNull();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void consumeResetTokenRejectsUsedOrExpired() {
        PasswordResetToken used = new PasswordResetToken();
        used.setUsedAt(Instant.now());
        used.setExpiresAt(Instant.now().plusSeconds(1800));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> tokenService().consumePasswordResetToken("used"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESET_TOKEN_INVALID));

        PasswordResetToken expired = new PasswordResetToken();
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> tokenService().consumePasswordResetToken("expired"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESET_TOKEN_INVALID));
    }
}
