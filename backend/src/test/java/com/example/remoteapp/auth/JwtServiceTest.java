package com.example.remoteapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho {@link JwtService} — sinh/kiểm tra access token.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
 */
class JwtServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters!!";

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void generateAndParseRoundTrip() {
        JwtService jwtService = new JwtService(SECRET, 15);
        UUID userId = UUID.randomUUID();

        JwtService.AccessToken accessToken = jwtService.generateAccessToken(userId, "user@example.com");
        JwtService.JwtClaims claims = jwtService.parseAccessToken(accessToken.value());

        assertThat(claims.userId()).isEqualTo(userId);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(accessToken.expiresAt()).isAfter(java.time.Instant.now());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void expiredTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, -1);
        JwtService.AccessToken accessToken = jwtService.generateAccessToken(UUID.randomUUID(), "user@example.com");

        assertThatThrownBy(() -> jwtService.parseAccessToken(accessToken.value()))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_EXPIRED));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void tamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 15);
        JwtService other = new JwtService(SECRET + "x", 15);
        String foreignToken = other.generateAccessToken(UUID.randomUUID(), "user@example.com").value();

        assertThatThrownBy(() -> jwtService.parseAccessToken(foreignToken))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID));

        assertThatThrownBy(() -> jwtService.parseAccessToken("not-a-jwt"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOKEN_INVALID));
    }
}
