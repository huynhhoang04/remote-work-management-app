package com.example.remoteapp.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request tạo bình luận kèm @mention.
 *
 * @param content        nội dung (1–2000)
 * @param mentionUserIds thành viên dự án được nhắc (client gửi, không parse text)
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record CommentRequest(
        @NotBlank(message = "Nội dung không được để trống")
        @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
        String content,

        List<UUID> mentionUserIds) {
}
