package com.example.remoteapp.auth;

import com.example.remoteapp.auth.dto.ChangePasswordRequest;
import com.example.remoteapp.auth.dto.ForgotPasswordRequest;
import com.example.remoteapp.auth.dto.LoginRequest;
import com.example.remoteapp.auth.dto.RefreshRequest;
import com.example.remoteapp.auth.dto.RegisterRequest;
import com.example.remoteapp.auth.dto.ResetPasswordRequest;
import com.example.remoteapp.auth.dto.TokenResponse;
import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho xác thực + đổi mật khẩu (plan 02 §2–§3).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §2
 */
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param authService service xác thực
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Đăng ký tài khoản (FR-A01).
     *
     * @param request thông tin đăng ký (validate DTO)
     * @return 201 + hồ sơ người dùng
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/users/me")).body(authService.register(request));
    }

    /**
     * Đăng nhập (FR-A02).
     *
     * @param request email + mật khẩu
     * @return 200 + access/refresh token
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    @PostMapping("/auth/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Làm mới phiên đăng nhập (FR-G04).
     *
     * @param request refresh token
     * @return 200 + token mới (xoay vòng)
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Đăng xuất — thu hồi phiên của người dùng.
     *
     * @param request refresh token hiện tại
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        UUID actorId = SecurityUtils.currentUserId();
        authService.logout(actorId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Quên mật khẩu (FR-A03) — luôn trả 204 để không lộ email tồn tại.
     *
     * @param request email cần khôi phục
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đặt lại mật khẩu bằng token email (FR-A03).
     *
     * @param request token + mật khẩu mới
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    @PostMapping("/auth/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Đổi mật khẩu khi đã đăng nhập (FR-A04).
     *
     * <p>Endpoint thuộc nhóm {@code /users} nhưng xử lý tại {@link AuthService}
     * để giữ dependency một chiều auth → user (plan 01 §4.2): đổi mật khẩu cần
     * thu hồi refresh token — thuộc phạm vi auth.</p>
     *
     * @param request mật khẩu hiện tại + mật khẩu mới
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @PostMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        UUID actorId = SecurityUtils.currentUserId();
        authService.changePassword(actorId, request);
        return ResponseEntity.noContent().build();
    }
}
