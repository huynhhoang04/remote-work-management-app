package com.example.remoteapp.user.dto;

import com.example.remoteapp.user.User;
import java.util.UUID;

/**
 * DTO hồ sơ người dùng — không bao giờ chứa mật khẩu.
 *
 * @param id        id người dùng
 * @param email     email
 * @param fullName  họ tên
 * @param avatarUrl URL avatar (null nếu chưa có)
 * @reference docs/plan-backend/02-danh-sach-api.md §3
 */
public record UserResponse(UUID id, String email, String fullName, String avatarUrl) {

    /**
     * Ánh xạ entity sang DTO.
     *
     * @param user entity người dùng
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1 (mapping thủ công)
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getAvatarUrl());
    }
}
