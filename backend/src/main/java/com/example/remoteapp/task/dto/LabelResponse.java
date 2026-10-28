package com.example.remoteapp.task.dto;

import com.example.remoteapp.task.Label;

/**
 * DTO nhãn dự án.
 *
 * @param id    id nhãn
 * @param name  tên nhãn
 * @param color màu hiển thị
 * @reference docs/plan-backend/02-danh-sach-api.md §5
 */
public record LabelResponse(java.util.UUID id, String name, String color) {

    /**
     * Ánh xạ entity sang DTO.
     *
     * @param label entity nhãn
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1 (mapping thủ công)
     */
    public static LabelResponse from(Label label) {
        return new LabelResponse(label.getId(), label.getName(), label.getColor());
    }
}
