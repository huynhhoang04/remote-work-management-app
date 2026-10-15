package com.example.remoteapp.project;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.dto.AuditLogResponse;
import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.project.dto.InviteRequest;
import com.example.remoteapp.project.dto.InviteResponse;
import com.example.remoteapp.project.dto.JoinRequest;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.project.dto.ProjectRequest;
import com.example.remoteapp.project.dto.ProjectResponse;
import com.example.remoteapp.project.dto.RoleRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller cho dự án, thành viên, link mời và audit log (plan 02 §4, §10).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §4
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final InviteService inviteService;

    /**
     * Danh sách dự án của người dùng (FR-B02).
     *
     * @return danh sách dự án kèm vai trò
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return ResponseEntity.ok(projectService.listForUser(SecurityUtils.currentUserId()));
    }

    /**
     * Tạo dự án mới (FR-B02) — người tạo là OWNER.
     *
     * @param request tên + mô tả (validate DTO)
     * @return 201 + dự án mới
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.create(SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + response.id())).body(response);
    }

    /**
     * Xem chi tiết dự án (FR-B02).
     *
     * @param id id dự án
     * @return dự án kèm vai trò của người xem
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.get(id, SecurityUtils.currentUserId()));
    }

    /**
     * Sửa dự án (FR-B02) — Owner/Admin.
     *
     * @param id      id dự án
     * @param request các trường cần sửa
     * @return dự án sau khi sửa
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @PatchMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID id,
                                                         @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, SecurityUtils.currentUserId(), request));
    }

    /**
     * Xoá dự án — soft delete, chỉ Owner (FR-B02).
     *
     * @param id id dự án
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        projectService.delete(id, SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Danh sách thành viên dự án (FR-B03).
     *
     * @param id id dự án
     * @return danh sách thành viên
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @GetMapping("/projects/{id}/members")
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.listMembers(id, SecurityUtils.currentUserId()));
    }

    /**
     * Đổi vai trò thành viên (FR-G01) — Owner/Admin.
     *
     * @param id     id dự án
     * @param userId thành viên
     * @param request vai trò mới
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @PatchMapping("/projects/{id}/members/{userId}")
    public ResponseEntity<Void> changeRole(@PathVariable UUID id, @PathVariable UUID userId,
                                           @Valid @RequestBody RoleRequest request) {
        projectService.changeRole(id, SecurityUtils.currentUserId(), userId, request.role());
        return ResponseEntity.noContent().build();
    }

    /**
     * Xoá thành viên / rời dự án (FR-B03).
     *
     * @param id     id dự án
     * @param userId thành viên bị xoá
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @DeleteMapping("/projects/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        projectService.removeMember(id, SecurityUtils.currentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sinh link mời dự án (FR-B03) — Owner/Admin.
     *
     * @param id      id dự án
     * @param request hạn dùng (tuỳ chọn)
     * @return 201 + link mời
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @PostMapping("/projects/{id}/invites")
    public ResponseEntity<InviteResponse> createInvite(@PathVariable UUID id,
                                                       @RequestBody InviteRequest request) {
        InviteResponse response = inviteService.create(SecurityUtils.currentUserId(), id, request);
        return ResponseEntity.created(URI.create("/api/v1/projects/" + id + "/invites")).body(response);
    }

    /**
     * Danh sách link mời còn hiệu lực (FR-B03) — Owner/Admin.
     *
     * @param id id dự án
     * @return danh sách link mời
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @GetMapping("/projects/{id}/invites")
    public ResponseEntity<List<InviteResponse>> listInvites(@PathVariable UUID id) {
        return ResponseEntity.ok(inviteService.list(SecurityUtils.currentUserId(), id));
    }

    /**
     * Thu hồi link mời (FR-B03) — Owner/Admin.
     *
     * @param id       id dự án
     * @param inviteId link mời
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @DeleteMapping("/projects/{id}/invites/{inviteId}")
    public ResponseEntity<Void> revokeInvite(@PathVariable UUID id, @PathVariable UUID inviteId) {
        inviteService.revoke(SecurityUtils.currentUserId(), id, inviteId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Tham gia dự án bằng mã mời (FR-B03).
     *
     * @param request mã mời (validate DTO)
     * @return dự án đã tham gia
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @PostMapping("/projects/join")
    public ResponseEntity<ProjectResponse> joinProject(@Valid @RequestBody JoinRequest request) {
        return ResponseEntity.ok(inviteService.join(SecurityUtils.currentUserId(), request));
    }

    /**
     * Xem audit log dự án (FR-G05) — chỉ Owner.
     *
     * @param id       id dự án
     * @param action   lọc theo hành động (tuỳ chọn)
     * @param pageable phân trang
     * @return trang audit log
     * @reference docs/plan-backend/02-danh-sach-api.md §10
     */
    @GetMapping("/projects/{id}/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(@PathVariable UUID id,
                                                                @RequestParam(required = false)
                                                                AuditAction action,
                                                                Pageable pageable) {
        return ResponseEntity.ok(
                projectService.listAuditLogs(id, SecurityUtils.currentUserId(), action, pageable));
    }
}
