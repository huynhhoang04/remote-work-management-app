package com.example.remoteapp.project;

/**
 * Vai trò của thành viên trong một dự án (RBAC — FR-G01).
 *
 * <p>Thứ tự khai báo = thứ tự quyền giảm dần: {@code OWNER > ADMIN > MEMBER > VIEWER}.
 * Viewer chỉ được xem (không thao tác ghi) — plan 03 §B2.</p>
 *
 * @reference docs/04-thiet-ke-backend.md §9
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
 */
public enum ProjectRole {

    /** Chủ dự án: toàn quyền, không bị xoá/đổi vai trò. */
    OWNER,
    /** Quản trị dự án: quản lý thành viên, task, dữ liệu. */
    ADMIN,
    /** Thành viên: tạo/sửa task, bình luận, chat. */
    MEMBER,
    /** Người chỉ xem — mọi thao tác ghi bị từ chối (403). */
    VIEWER;

    /**
     * So sánh quyền: vai trò hiện tại có ít nhất bằng vai trò yêu cầu không.
     *
     * @param required vai trò tối thiểu cần có
     * @return true nếu đủ quyền
     * @reference docs/04-thiet-ke-backend.md §9
     */
    public boolean isAtLeast(ProjectRole required) {
        return this.ordinal() <= required.ordinal();
    }
}
