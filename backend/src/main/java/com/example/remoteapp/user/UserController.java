package com.example.remoteapp.user;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.user.dto.UpdateProfileRequest;
import com.example.remoteapp.user.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
public class UserController {

    private final UserService userService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param userService service hồ sơ người dùng
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    /**
     * Upload avatar (multipart, ảnh ≤ 5MB) — cập nhật luôn hồ sơ (FR-A04).
     *
     * @param file file ảnh
     * @return hồ sơ sau khi cập nhật
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.updateAvatar(SecurityUtils.currentUserId(), file));
    }
}
