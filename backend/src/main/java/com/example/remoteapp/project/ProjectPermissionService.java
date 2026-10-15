package com.example.remoteapp.project;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kiểm tra phân quyền theo dự án — **thực thi ở backend** cho mọi thao tác
 * (UI chỉ ẩn nút, plan 03 §B2).
 *
 * <p>Mọi module nghiệp vụ (task, chat, file, stats...) đều kiểm tra quyền
 * qua service này — không query trực tiếp bảng {@code project_members}.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
 * @reference docs/04-thiet-ke-backend.md §9
 */
@Service
@RequiredArgsConstructor
public class ProjectPermissionService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * Yêu cầu người dùng có vai trò tối thiểu trong dự án.
     *
     * @param projectId  dự án
     * @param userId     người yêu cầu
     * @param minRole    vai trò tối thiểu
     * @return quan hệ thành viên (kèm vai trò thực tế)
     * @throws AppException PROJECT_NOT_FOUND nếu dự án không tồn tại/đã xoá,
     *                      PROJECT_FORBIDDEN nếu không phải thành viên hoặc thiếu quyền
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    @Transactional(readOnly = true)
    public ProjectMember requireRole(UUID projectId, UUID userId, ProjectRole minRole) {
        requireProject(projectId);
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_FORBIDDEN,
                        "Bạn không phải thành viên của dự án này"));
        if (!member.getRole().isAtLeast(minRole)) {
            throw new AppException(ErrorCode.PROJECT_FORBIDDEN,
                    "Bạn không có quyền thực hiện thao tác này trong dự án");
        }
        return member;
    }

    /**
     * Yêu cầu người dùng là thành viên dự án (mọi vai trò, kể cả VIEWER).
     *
     * @param projectId dự án
     * @param userId    người yêu cầu
     * @return quan hệ thành viên
     * @throws AppException PROJECT_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    @Transactional(readOnly = true)
    public ProjectMember requireMember(UUID projectId, UUID userId) {
        return requireRole(projectId, userId, ProjectRole.VIEWER);
    }

    /**
     * Kiểm tra dự án tồn tại và chưa bị soft delete.
     *
     * @param projectId dự án
     * @return entity {@link Project}
     * @throws AppException PROJECT_NOT_FOUND nếu không tồn tại/đã xoá
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1
     */
    @Transactional(readOnly = true)
    public Project requireProject(UUID projectId) {
        return projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND, "Không tìm thấy dự án"));
    }
}
