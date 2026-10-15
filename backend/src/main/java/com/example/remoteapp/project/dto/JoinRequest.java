package com.example.remoteapp.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request tham gia dự án bằng mã mời (FR-B03).
 *
 * @param code mã mời từ link
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
public record JoinRequest(
        @NotBlank(message = "Mã mời không được để trống")
        @Size(max = 12, message = "Mã mời tối đa 12 ký tự")
        String code) {
}
