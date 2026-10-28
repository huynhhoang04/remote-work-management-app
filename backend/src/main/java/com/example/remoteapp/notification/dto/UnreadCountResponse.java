package com.example.remoteapp.notification.dto;

/**
 * DTO số thông báo chưa đọc.
 *
 * @param count số chưa đọc
 * @reference docs/plan-backend/02-danh-sach-api.md §9
 */
public record UnreadCountResponse(long count) {
}
