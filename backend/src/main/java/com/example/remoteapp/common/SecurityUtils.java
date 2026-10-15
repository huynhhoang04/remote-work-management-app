package com.example.remoteapp.common;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Tiện ích lấy thông tin người dùng hiện tại từ SecurityContext
 * (principal là userId do {@code JwtAuthenticationFilter} gắn vào).
 *
 * <p>Controller lấy userId từ đây rồi truyền tường minh vào service —
 * service không đọc SecurityContext nên dễ unit test (plan 01 §4.1).</p>
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1, §5
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Lấy userId của người dùng đã xác thực trong request hiện tại.
     *
     * @return userId (UUID) từ JWT
     * @throws AppException AUTH_UNAUTHORIZED nếu chưa xác thực
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §5
     */
    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Chưa đăng nhập");
        }
        try {
            return UUID.fromString(authentication.getPrincipal().toString());
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID, "Token không hợp lệ");
        }
    }
}
