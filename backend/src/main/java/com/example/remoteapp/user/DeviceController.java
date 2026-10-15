package com.example.remoteapp.user;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.user.dto.RegisterDeviceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho device token FCM (plan 02 §3).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §3
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DeviceController {

    private final UserService userService;

    /**
     * Đăng ký FCM token của thiết bị (nền cho push notification).
     *
     * @param request FCM token + nền tảng (validate DTO)
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @PostMapping("/devices/token")
    public ResponseEntity<Void> registerDevice(@Valid @RequestBody RegisterDeviceRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        userService.registerDevice(userId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Huỷ đăng ký FCM token (khi logout / token hỏng).
     *
     * @param request FCM token cần huỷ
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    @DeleteMapping("/devices/token")
    public ResponseEntity<Void> removeDevice(@Valid @RequestBody RemoveDeviceRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        userService.removeDevice(userId, request.fcmToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Request huỷ FCM token.
     *
     * @param fcmToken token cần huỷ
     * @reference docs/plan-backend/02-danh-sach-api.md §3
     */
    public record RemoveDeviceRequest(
            @NotBlank(message = "FCM token không được để trống")
            String fcmToken) {
    }
}
