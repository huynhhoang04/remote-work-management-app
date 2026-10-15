package com.example.remoteapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho {@link LoginRateLimiter} — giới hạn 10 lần/phút/email (NFR-S05).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
 */
class LoginRateLimiterTest {

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void blocksAfterMaxAttemptsWithinWindow() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, 60);

        limiter.hit("user@example.com");
        limiter.hit("user@example.com");
        limiter.hit("user@example.com");

        assertThatThrownBy(() -> limiter.hit("user@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_RATE_LIMITED));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void differentKeysAreCountedSeparately() {
        LoginRateLimiter limiter = new LoginRateLimiter(1, 60);

        limiter.hit("a@example.com");

        assertThatCode(() -> limiter.hit("b@example.com")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.hit("a@example.com")).isInstanceOf(AppException.class);
    }
}
