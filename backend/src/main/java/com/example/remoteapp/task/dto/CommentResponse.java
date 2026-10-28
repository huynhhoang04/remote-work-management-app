package com.example.remoteapp.task.dto;

import com.example.remoteapp.task.TaskComment;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO bình luận task.
 *
 * @param id        id bình luận
 * @param taskId    task chứa bình luận
 * @param userId    tác giả
 * @param fullName  họ tên tác giả
 * @param content   nội dung
 * @param createdAt thời điểm tạo
 * @param updatedAt thời điểm cập nhật
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record CommentResponse(UUID id, UUID taskId, UUID userId, String fullName, String content,
                              Instant createdAt, Instant updatedAt) {

    /**
     * Ánh xạ entity + tên tác giả sang DTO.
     *
     * @param comment  entity bình luận
     * @param fullName họ tên tác giả
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1 (mapping thủ công)
     */
    public static CommentResponse from(TaskComment comment, String fullName) {
        return new CommentResponse(comment.getId(), comment.getTaskId(), comment.getUserId(),
                fullName, comment.getContent(), comment.getCreatedAt(), comment.getUpdatedAt());
    }
}
