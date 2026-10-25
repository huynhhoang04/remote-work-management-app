package com.example.remoteapp.project;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.project.dto.InviteRequest;
import com.example.remoteapp.project.dto.InviteResponse;
import com.example.remoteapp.project.dto.JoinRequest;
import com.example.remoteapp.project.dto.ProjectResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ link mời dự án (FR-B03 — rút gọn: chỉ còn link/mã mời, không email).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
 */
@Service
public class InviteService {

    private static final String ENTITY_TYPE = "INVITE";
    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 12;
    private static final String INVITE_URL_FORMAT = "remoteapp://join?code=%s";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProjectInviteRepository inviteRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectPermissionService permissionService;
    private final AuditService auditService;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param inviteRepository  repository link mời
     * @param memberRepository  repository thành viên dự án
     * @param permissionService kiểm tra phân quyền theo dự án
     * @param auditService      ghi audit log
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public InviteService(ProjectInviteRepository inviteRepository, ProjectMemberRepository memberRepository,
                         ProjectPermissionService permissionService, AuditService auditService) {
        this.inviteRepository = inviteRepository;
        this.memberRepository = memberRepository;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    /**
     * Sinh link mời mới — Owner/Admin (FR-B03).
     *
     * @param actorId   người tạo link
     * @param projectId dự án
     * @param request   hạn dùng (tuỳ chọn)
     * @return thông tin link mời (mã + deep-link)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    @Transactional
    public InviteResponse create(UUID actorId, UUID projectId, InviteRequest request) {
        permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN);
        ProjectInvite invite = new ProjectInvite();
        invite.setProjectId(projectId);
        invite.setCode(generateCode());
        invite.setCreatedBy(actorId);
        invite.setExpiresAt(request.expiresAt());
        inviteRepository.save(invite);
        auditService.record(actorId, projectId, AuditAction.INVITE_CREATED, ENTITY_TYPE,
                invite.getId(), null);
        return toResponse(invite);
    }

    /**
     * Danh sách link mời còn hiệu lực của dự án — Owner/Admin.
     *
     * @param actorId   người yêu cầu
     * @param projectId dự án
     * @return danh sách link mời
     * @reference docs/plan-backend/02-danh-sach-api.md §4
     */
    @Transactional(readOnly = true)
    public List<InviteResponse> list(UUID actorId, UUID projectId) {
        permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN);
        return inviteRepository.findByProjectIdAndRevokedFalseOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Thu hồi link mời — Owner/Admin (thành viên đã tham gia không bị ảnh hưởng).
     *
     * @param actorId   người yêu cầu
     * @param projectId dự án
     * @param inviteId  link cần thu hồi
     * @throws AppException INVITE_INVALID nếu không tìm thấy link
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    @Transactional
    public void revoke(UUID actorId, UUID projectId, UUID inviteId) {
        permissionService.requireRole(projectId, actorId, ProjectRole.ADMIN);
        ProjectInvite invite = inviteRepository.findByIdAndProjectId(inviteId, projectId)
                .orElseThrow(() -> new AppException(ErrorCode.INVITE_INVALID, "Không tìm thấy link mời"));
        invite.setRevoked(true);
        inviteRepository.save(invite);
        auditService.record(actorId, projectId, AuditAction.INVITE_REVOKED, ENTITY_TYPE, inviteId, null);
    }

    /**
     * Tham gia dự án bằng mã mời (FR-B03) — idempotent với người đã là thành viên.
     *
     * @param actorId người tham gia
     * @param request mã mời
     * @return dự án vừa tham gia (vai trò MEMBER)
     * @throws AppException INVITE_INVALID nếu mã sai/thu hồi/hết hạn/dự án đã xoá
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    @Transactional
    public ProjectResponse join(UUID actorId, JoinRequest request) {
        ProjectInvite invite = inviteRepository.findByCode(request.code().trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.INVITE_INVALID, "Mã mời không hợp lệ"));
        if (invite.isRevoked()
                || (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now()))) {
            throw new AppException(ErrorCode.INVITE_INVALID, "Link mời đã bị thu hồi hoặc hết hạn");
        }
        Project project = permissionService.requireProject(invite.getProjectId());
        if (!memberRepository.existsByProjectIdAndUserId(project.getId(), actorId)) {
            ProjectMember member = new ProjectMember();
            member.setProjectId(project.getId());
            member.setUserId(actorId);
            member.setRole(ProjectRole.MEMBER);
            memberRepository.save(member);
            auditService.record(actorId, project.getId(), AuditAction.MEMBER_JOINED, "MEMBER",
                    actorId, "via=invite");
        }
        return ProjectResponse.from(project, ProjectRole.MEMBER.name());
    }

    /**
     * Sinh mã mời ngẫu nhiên 12 ký tự [A-Z0-9].
     *
     * @return mã mời
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
     */
    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    /**
     * Ánh xạ entity sang DTO response.
     *
     * @param invite link mời
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    private InviteResponse toResponse(ProjectInvite invite) {
        return new InviteResponse(invite.getId(), invite.getCode(),
                INVITE_URL_FORMAT.formatted(invite.getCode()), invite.getExpiresAt());
    }
}
