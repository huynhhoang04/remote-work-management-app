package com.example.remoteapp.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request đăng ký FCM token của thiết bị (nền cho push notification).
 *
 * @param fcmToken token FCM do Firebase cấp
 * @param platform nền tảng: ANDROID / IOS
 * @reference docs/plan-backend/02-danh-sach-api.md §3
 */
public record RegisterDeviceRequest(
        @NotBlank(message = "FCM token không được để trống")
        @Size(max = 500, message = "FCM token tối đa 500 ký tự")
        String fcmToken,

        @NotBlank(message = "Nền tảng không được để trống")
        @Size(max = 20, message = "Nền tảng tối đa 20 ký tự")
        String platform) {
}
