package com.example.remoteapp.auth;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Rate limit đăng nhập: tối đa {@code app.auth.login-max-attempts} lần/phút cho
 * mỗi email (NFR-S05 — chống dò mật khẩu).
 *
 * <p>Bộ đếm in-memory theo cửa sổ thời gian — đủ cho một instance backend
 * (plan 03 §A2).</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
 * @reference docs/04-thiet-ke-backend.md §10
 */
@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowSeconds;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Khởi tạo limiter từ cấu hình.
     *
     * @param maxAttempts   số lần tối đa trong một cửa sổ
     * @param windowSeconds độ dài cửa sổ (giây)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §7
     */
    public LoginRateLimiter(@Value("${app.auth.login-max-attempts}") int maxAttempts,
                            @Value("${app.auth.login-window-seconds}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    /**
     * Ghi nhận một lần đăng nhập theo khoá (email) và chặn nếu vượt ngưỡng.
     *
     * @param key khoá đếm (email đã lowercase)
     * @throws AppException AUTH_RATE_LIMITED nếu vượt quá số lần cho phép
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    public void hit(String key) {
        long now = Instant.now().getEpochSecond();
        Window window = windows.computeIfAbsent(key, k -> new Window(now));
        synchronized (window) {
            if (now - window.startEpochSecond >= windowSeconds) {
                window.startEpochSecond = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > maxAttempts) {
                throw new AppException(ErrorCode.AUTH_RATE_LIMITED,
                        "Quá nhiều lần đăng nhập, vui lòng thử lại sau ít phút");
            }
        }
    }

    /** Cửa sổ đếm đơn giản (mutable, đồng bộ theo khoá). */
    private static final class Window {
        private long startEpochSecond;
        private int count;

        private Window(long startEpochSecond) {
            this.startEpochSecond = startEpochSecond;
        }
    }
}
