package com.example.remoteapp.task.dto;

import com.example.remoteapp.file.dto.FileResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO task trả về client (kèm assignee, nhãn, subtask, file đính kèm).
 *
 * @param id          id task
 * @param projectId   dự án
 * @param parentTaskId task cha (null nếu task gốc)
 * @param title       tiêu đề
 * @param description mô tả
 * @param status      trạng thái
 * @param priority    độ ưu tiên
 * @param startDate   ngày bắt đầu
 * @param deadline    hạn hoàn thành
 * @param position    vị trí Kanban
 * @param createdBy   người tạo
 * @param completedAt thời điểm hoàn thành
 * @param version     phiên bản (optimistic locking)
 * @param assignees   người được gán
 * @param labels      nhãn
 * @param subtasks    checklist con
 * @param attachments file đính kèm
 * @param commentCount số bình luận
 * @param createdAt   thời điểm tạo
 * @param updatedAt   thời điểm cập nhật
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record TaskResponse(UUID id, UUID projectId, UUID parentTaskId, String title, String description,
                           String status, String priority, Instant startDate, Instant deadline,
                           double position, UUID createdBy, Instant completedAt, long version,
                           List<AssigneeResponse> assignees, List<LabelResponse> labels,
                           List<SubtaskResponse> subtasks, List<FileResponse> attachments,
                           long commentCount, Instant createdAt, Instant updatedAt) {

    /**
     * Người được gán task.
     *
     * @param userId   id người dùng
     * @param fullName họ tên
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    public record AssigneeResponse(UUID userId, String fullName) {
    }

    /**
     * Checklist con của task.
     *
     * @param id       id subtask
     * @param title    tiêu đề
     * @param done     đã hoàn thành chưa
     * @param position vị trí
     * @reference docs/plan-backend/02-danh-sach-api.md §5
     */
    public record SubtaskResponse(UUID id, String title, boolean done, double position) {
    }
}
