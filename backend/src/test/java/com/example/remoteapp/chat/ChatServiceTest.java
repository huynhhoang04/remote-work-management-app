package com.example.remoteapp.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.remoteapp.chat.dto.ConversationResponse;
import com.example.remoteapp.chat.dto.CreateDirectRequest;
import com.example.remoteapp.chat.dto.MessageResponse;
import com.example.remoteapp.chat.dto.SendMessageRequest;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.notification.NotificationService;
import com.example.remoteapp.notification.NotificationType;
import com.example.remoteapp.project.ProjectMember;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.project.ProjectRole;
import com.example.remoteapp.project.ProjectService;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.user.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Unit test cho {@link ChatService} — idempotent, quyền, unread, broadcast, notify.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1, §F2
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationMemberRepository memberRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ProjectService projectService;
    @Mock private ProjectPermissionService permissionService;
    @Mock private UserService userService;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;
    private UUID actorId;
    private UUID otherId;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(conversationRepository, memberRepository, messageRepository,
                projectService, permissionService, userService, notificationService,
                messagingTemplate);
        actorId = UUID.randomUUID();
        otherId = UUID.randomUUID();
    }

    private Conversation conversation(ConversationType type, UUID id, UUID projectId) {
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setType(type);
        conversation.setProjectId(projectId);
        return conversation;
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1 */
    @Test
    void createDirect_isIdempotentForSamePair() {
        UUID conversationId = UUID.randomUUID();
        Conversation existing = conversation(ConversationType.DIRECT, conversationId, null);
        ConversationMember member = new ConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(actorId);
        when(userService.namesByIds(Set.of(otherId))).thenReturn(Map.of(otherId, "Other"));
        when(conversationRepository.findByDirectKey(anyString())).thenReturn(Optional.of(existing));
        when(memberRepository.existsByConversationIdAndUserId(conversationId, actorId)).thenReturn(true);
        when(memberRepository.existsByConversationIdAndUserId(conversationId, otherId)).thenReturn(true);
        when(memberRepository.findByConversationIdAndUserId(conversationId, actorId))
                .thenReturn(Optional.of(member));
        when(messageRepository.findFirstByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                conversationId)).thenReturn(Optional.empty());
        when(messageRepository.countByConversationIdAndSenderIdNot(conversationId, actorId))
                .thenReturn(0L);

        ConversationResponse response = chatService.createDirect(actorId,
                new CreateDirectRequest(otherId));

        assertThat(response.id()).isEqualTo(conversationId);
        assertThat(response.type()).isEqualTo("DIRECT");
        assertThat(response.name()).isEqualTo("Other");
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2 */
    @Test
    void send_withSameClientMessageIdReturnsExistingMessage() {
        UUID conversationId = UUID.randomUUID();
        UUID clientMessageId = UUID.randomUUID();
        Conversation conversation = conversation(ConversationType.DIRECT, conversationId, null);
        Message existing = new Message();
        existing.setId(UUID.randomUUID());
        existing.setConversationId(conversationId);
        existing.setSenderId(actorId);
        existing.setContent("hello");
        existing.setClientMessageId(clientMessageId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserId(conversationId, actorId))
                .thenReturn(true);
        when(messageRepository.findByConversationIdAndClientMessageId(conversationId,
                clientMessageId)).thenReturn(Optional.of(existing));
        when(userService.namesByIds(Set.of(actorId))).thenReturn(Map.of(actorId, "Actor"));

        MessageResponse response = chatService.send(conversationId, actorId,
                new SendMessageRequest("hello", null, clientMessageId));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(messageRepository, never()).save(any(Message.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2 */
    @Test
    void send_broadcastsAndNotifiesEveryoneExceptSender() {
        UUID conversationId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Conversation conversation = conversation(ConversationType.GROUP, conversationId, projectId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(permissionService.requireRole(projectId, actorId, ProjectRole.MEMBER))
                .thenReturn(new ProjectMember());
        when(projectService.listMembers(projectId, actorId)).thenReturn(List.of(
                new ProjectMemberResponse(actorId, "Actor", "a@example.com", "OWNER",
                        Instant.now()),
                new ProjectMemberResponse(otherId, "Other", "o@example.com", "MEMBER",
                        Instant.now())));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(userService.namesByIds(Set.of(actorId))).thenReturn(Map.of(actorId, "Actor"));

        MessageResponse response = chatService.send(conversationId, actorId,
                new SendMessageRequest("Xin chào", null, UUID.randomUUID()));

        assertThat(response.content()).isEqualTo("Xin chào");
        verify(messagingTemplate).convertAndSend(eq("/topic/conversations." + conversationId),
                any(Object.class));
        verify(notificationService).notify(eq(otherId), eq(NotificationType.MESSAGE), anyString(),
                anyString(), eq("CONVERSATION"), eq(conversationId), isNull());
        verify(notificationService, never()).notify(eq(actorId), any(), any(), any(), any(), any(),
                any());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2 */
    @Test
    void send_rejectsViewerInGroupConversation() {
        UUID conversationId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        Conversation conversation = conversation(ConversationType.GROUP, conversationId, projectId);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(permissionService.requireRole(projectId, actorId, ProjectRole.MEMBER))
                .thenThrow(new AppException(ErrorCode.PROJECT_FORBIDDEN));

        assertThatThrownBy(() -> chatService.send(conversationId, actorId,
                new SendMessageRequest("hi", null, null)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PROJECT_FORBIDDEN));
        verify(messageRepository, never()).save(any(Message.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1 */
    @Test
    void send_rejectsNonMemberOfDirectConversation() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = conversation(ConversationType.DIRECT, conversationId, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(memberRepository.existsByConversationIdAndUserId(conversationId, actorId))
                .thenReturn(false);

        assertThatThrownBy(() -> chatService.send(conversationId, actorId,
                new SendMessageRequest("hi", null, null)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONVERSATION_FORBIDDEN));
    }
}
