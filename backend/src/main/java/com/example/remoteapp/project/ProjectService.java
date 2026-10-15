package com.example.remoteapp.project;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.audit.dto.AuditLogResponse;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.project.dto.ProjectRequest;
import com.example.remoteapp.project.dto.ProjectResponse;
import com.example.remoteapp.user.User;
import com.example.remoteapp.user.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ dự án: CRUD, thành viên, vai trò (plan 03 §B1, §B2).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1, §B2
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String ENTITY_TYPE = "PROJECT";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPermissionService permissionService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    /**
     * Tạo dự án — người tạo trở thành OWNER + thành viên (FR-B02).
     *
     * @param actorId người tạo
     * @param request tên + mô tả (validate DTO)
     * @return dự án mới
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1
     */
    @Transactional
    public ProjectResponse create(UUID actorId, ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setOwnerId(actorId);
        projectRepository.save(project);

        ProjectMember owner = new ProjectMember();
        owner.setProjectId(project.getId());
        owner.setUserId(actorId);
        owner.setRole(ProjectRole.OWNER);
        projectMemberRepository.save(owner);

        auditService.record(actorId, project.getId(), AuditAction.PROJECT_CREATED, ENTITY_TYPE,
                project.getId(), "name=" + project.getName());
        return ProjectResponse.from(project, ProjectRole.OWNER.name());
    }

    /**
     * Danh sách dự án mà người dùng là thành viên (FR-B02).
     *
     * @param actorId người yêu cầu
     * @return danh sách dự án kèm vai trò của người xem
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> listForUser(UUID actorId) {
        return projectRepository.findAllByMemberUserId(actorId).stream()
                .map(project -> ProjectResponse.from(project, roleOf(project.getId(), actorId)))
                .toList();
    }

    /**
     * Xem chi tiết dự án (mọi vai trò đều xem được — FR-G01).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @return dự án kèm vai trò
     * @throws AppException PROJECT_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId, UUID actorId) {
        Project project = permissionService.requireProject(projectId);
        ProjectMember member = permissionService.requireMember(projectId, actorId);
        return ProjectResponse.from(project, member.getRole().name());
    }

    /**
     * Sửa dự án — yêu cầu vai trò ADMIN trở lên (FR-B02).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @param request   các trường cần sửa
     * @return dự án sau khi sửa
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1
     */
    @Transactional
    public ProjectResponse update(UUID projectId, UUID actorId, ProjectRequest request) {
        ProjectMember member = permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN);
        Project project = permissionService.requireProject(projectId);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        projectRepository.save(project);
        auditService.record(actorId, projectId, AuditAction.PROJECT_UPDATED, ENTITY_TYPE, projectId, null);
        return ProjectResponse.from(project, member.getRole().name());
    }

    /**
     * Xoá dự án (soft delete) — chỉ OWNER (FR-B02).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1
     */
    @Transactional
    public void delete(UUID projectId, UUID actorId) {
        permissionService.requireRole(projectId, actorId, ProjectRole.OWNER);
        Project project = permissionService.requireProject(projectId);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);
        auditService.record(actorId, projectId, AuditAction.PROJECT_DELETED, ENTITY_TYPE, projectId, null);
    }

    /**
     * Danh sách thành viên dự án (mọi thành viên xem được).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @return danh sách thành viên kèm thông tin hồ sơ
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(UUID projectId, UUID actorId) {
        permissionService.requireMember(projectId, actorId);
        Map<UUID, User> users = userRepository.findAllById(
                        projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                                .map(ProjectMember::getUserId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(member -> toMemberResponse(member, users.get(member.getUserId())))
                .toList();
    }

    /**
     * Đổi vai trò thành viên (Owner/Admin) — FR-G01.
     *
     * <p>Luật: không đổi vai trò của OWNER, không gán role OWNER
     * (chuyển giao chủ dự án để sau).</p>
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @param targetId  thành viên bị đổi vai trò
     * @param newRole   vai trò mới
     * @throws AppException MEMBER_ROLE_INVALID nếu vi phạm luật,
     *                      MEMBER_NOT_FOUND nếu không phải thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    @Transactional
    public void changeRole(UUID projectId, UUID actorId, UUID targetId, ProjectRole newRole) {
        permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN);
        if (newRole == ProjectRole.OWNER) {
            throw new AppException(ErrorCode.MEMBER_ROLE_INVALID, "Không thể gán vai trò OWNER trực tiếp");
        }
        ProjectMember target = findMember(projectId, targetId);
        if (target.getRole() == ProjectRole.OWNER) {
            throw new AppException(ErrorCode.MEMBER_ROLE_INVALID, "Không thể thay đổi vai trò của chủ dự án");
        }
        target.setRole(newRole);
        projectMemberRepository.save(target);
        auditService.record(actorId, projectId, AuditAction.ROLE_CHANGED, "MEMBER", targetId,
                "role=" + newRole.name());
    }

    /**
     * Xoá thành viên / rời dự án (FR-B03).
     *
     * <p>Luật: Owner/Admin xoá được người khác; thành viên tự rời dự án được
     * (kể cả VIEWER); không xoá được OWNER.</p>
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @param targetId  thành viên bị xoá
     * @throws AppException MEMBER_ROLE_INVALID nếu target là OWNER,
     *                      PROJECT_FORBIDDEN nếu thiếu quyền
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    @Transactional
    public void removeMember(UUID projectId, UUID actorId, UUID targetId) {
        if (!actorId.equals(targetId)) {
            permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN);
        } else {
            permissionService.requireMember(projectId, actorId);
        }
        ProjectMember target = findMember(projectId, targetId);
        if (target.getRole() == ProjectRole.OWNER) {
            throw new AppException(ErrorCode.MEMBER_ROLE_INVALID, "Không thể xoá chủ dự án khỏi dự án");
        }
        projectMemberRepository.delete(target);
        auditService.record(actorId, projectId, AuditAction.MEMBER_REMOVED, "MEMBER", targetId, null);
    }

    /**
     * Xem audit log của dự án — chỉ OWNER (FR-G05).
     *
     * @param projectId dự án
     * @param actorId   người yêu cầu
     * @param action    lọc theo hành động (tuỳ chọn)
     * @param pageable  phân trang
     * @return trang audit log
     * @reference docs/plan-backend/02-danh-sach-api.md §10
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listAuditLogs(UUID projectId, UUID actorId,
                                                AuditAction action, Pageable pageable) {
        permissionService.requireRole(projectId, actorId, ProjectRole.OWNER);
        return auditService.listByProject(projectId, action, pageable).map(AuditLogResponse::from);
    }

    /**
     * Lấy vai trò của người dùng trong dự án (dùng cho response).
     *
     * @param projectId dự án
     * @param userId    người dùng
     * @return tên vai trò
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    private String roleOf(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .map(member -> member.getRole().name())
                .orElse(ProjectRole.VIEWER.name());
    }

    /**
     * Tìm quan hệ thành viên hoặc ném lỗi.
     *
     * @param projectId dự án
     * @param userId    người dùng
     * @return {@link ProjectMember}
     * @throws AppException MEMBER_NOT_FOUND nếu không phải thành viên
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B2
     */
    private ProjectMember findMember(UUID projectId, UUID userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND,
                        "Người dùng này không phải thành viên dự án"));
    }

    /**
     * Ghép thông tin thành viên + hồ sơ user.
     *
     * @param member quan hệ thành viên
     * @param user   hồ sơ (có thể null nếu user bị xoá)
     * @return DTO thành viên
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    private ProjectMemberResponse toMemberResponse(ProjectMember member, User user) {
        return new ProjectMemberResponse(
                member.getUserId(),
                user != null ? user.getFullName() : "(không rõ)",
                user != null ? user.getEmail() : "",
                member.getRole().name(),
                member.getJoinedAt());
    }
}
