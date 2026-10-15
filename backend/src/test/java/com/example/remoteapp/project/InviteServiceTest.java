package com.example.remoteapp.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.project.dto.InviteRequest;
import com.example.remoteapp.project.dto.InviteResponse;
import com.example.remoteapp.project.dto.JoinRequest;
import com.example.remoteapp.project.dto.ProjectResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link InviteService} — link mời + tham gia dự án (FR-B03).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3
 */
@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private ProjectInviteRepository inviteRepository;
    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private AuditService auditService;

    private ProjectPermissionService permissionService;
    private InviteService inviteService;

    @BeforeEach
    void setUp() {
        permissionService = new ProjectPermissionService(projectRepository, memberRepository);
        inviteService = new InviteService(inviteRepository, memberRepository, permissionService, auditService);
    }

    private Project project() {
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Do an Mobile");
        project.setOwnerId(UUID.randomUUID());
        return project;
    }

    private ProjectMember adminMember(UUID projectId, UUID userId) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setRole(ProjectRole.ADMIN);
        return member;
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3 */
    @Test
    void create_generatesRandomCode() {
        Project project = project();
        UUID actorId = UUID.randomUUID();
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(memberRepository.findByProjectIdAndUserId(project.getId(), actorId))
                .thenReturn(Optional.of(adminMember(project.getId(), actorId)));
        when(inviteRepository.save(any(ProjectInvite.class))).thenAnswer(invocation -> {
            ProjectInvite saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        InviteResponse response = inviteService.create(actorId, project.getId(), new InviteRequest(null));

        assertThat(response.code()).hasSize(12).matches("[A-Z0-9]{12}");
        assertThat(response.inviteUrl()).contains(response.code());
        verify(auditService).record(eq(actorId), eq(project.getId()), eq(AuditAction.INVITE_CREATED),
                eq("INVITE"), eq(response.id()), any());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3 */
    @Test
    void join_rejectsInvalidRevokedOrExpiredCodes() {
        when(inviteRepository.findByCode("BADCODE12345")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> inviteService.join(UUID.randomUUID(), new JoinRequest("BADCODE12345")))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVITE_INVALID));

        ProjectInvite revoked = new ProjectInvite();
        revoked.setRevoked(true);
        when(inviteRepository.findByCode("REVOKEDCODE12")).thenReturn(Optional.of(revoked));
        assertThatThrownBy(() -> inviteService.join(UUID.randomUUID(), new JoinRequest("REVOKEDCODE12")))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVITE_INVALID));

        ProjectInvite expired = new ProjectInvite();
        expired.setRevoked(false);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(inviteRepository.findByCode("EXPIREDCODE12")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> inviteService.join(UUID.randomUUID(), new JoinRequest("EXPIREDCODE12")))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVITE_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3 */
    @Test
    void join_addsMemberAndAudits() {
        Project project = project();
        ProjectInvite invite = new ProjectInvite();
        invite.setProjectId(project.getId());
        invite.setRevoked(false);
        UUID joinerId = UUID.randomUUID();
        when(inviteRepository.findByCode("VALIDCODE1234")).thenReturn(Optional.of(invite));
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(memberRepository.existsByProjectIdAndUserId(project.getId(), joinerId)).thenReturn(false);
        when(memberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response = inviteService.join(joinerId, new JoinRequest("VALIDCODE1234"));

        assertThat(response.id()).isEqualTo(project.getId());
        assertThat(response.role()).isEqualTo("MEMBER");
        verify(memberRepository).save(any(ProjectMember.class));
        verify(auditService).record(eq(joinerId), eq(project.getId()), eq(AuditAction.MEMBER_JOINED),
                eq("MEMBER"), eq(joinerId), anyString());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B3 */
    @Test
    void join_isIdempotentForExistingMember() {
        Project project = project();
        ProjectInvite invite = new ProjectInvite();
        invite.setProjectId(project.getId());
        invite.setRevoked(false);
        UUID joinerId = UUID.randomUUID();
        when(inviteRepository.findByCode("VALIDCODE1234")).thenReturn(Optional.of(invite));
        when(projectRepository.findByIdAndDeletedAtIsNull(project.getId())).thenReturn(Optional.of(project));
        when(memberRepository.existsByProjectIdAndUserId(project.getId(), joinerId)).thenReturn(true);

        inviteService.join(joinerId, new JoinRequest("VALIDCODE1234"));

        verify(memberRepository, never()).save(any(ProjectMember.class));
    }
}
