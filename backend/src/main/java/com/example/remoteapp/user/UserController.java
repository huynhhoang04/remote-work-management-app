package com.example.remoteapp.user;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.user.dto.UpdateProfileRequest;
import com.example.remoteapp.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho hồ sơ người dùng (plan 02 §3).
 *
 * <p>Endpoint đổi mật khẩu nằm ở {@code AuthController}
 * (giữ dependency auth → user, plan 01 §4.2).</p>
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §3
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Xem hồ sơ của người dùng đang đăng nhập (FR-A04).
     *
     * @return hồ sơ DTO
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getProfile() {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    /**
     * Cập nhật hồ sơ (FR-A04).
     *
     * @param request các trường cần sửa (validate DTO)
     * @return hồ sơ sau khi cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @PatchMapping("/users/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }
}
