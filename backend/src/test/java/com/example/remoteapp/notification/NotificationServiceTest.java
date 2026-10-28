package com.example.remoteapp.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.user.UserService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Unit test cho {@link NotificationService} — dedupe, WS, push, quyền đọc.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserService userService;
    @Mock private PushSender pushSender;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;
    private UUID userId;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userService,
                pushSender, messagingTemplate);
        userId = UUID.randomUUID();
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E1 */
    @Test
    void notify_skipsDuplicateDedupeKey() {
        when(notificationRepository.existsByDedupeKey("REMIND_1D:t:u")).thenReturn(true);

        notificationService.notify(userId, NotificationType.REMIND_1D, "T", "B", "TASK",
                UUID.randomUUID(), "REMIND_1D:t:u");

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(pushSender, messagingTemplate);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2 */
    @Test
    void notify_savesThenPushesAndSendsWebSocket() {
        UUID refId = UUID.randomUUID();
        when(notificationRepository.existsByDedupeKey("k")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(userService.listFcmTokens(userId)).thenReturn(List.of("token-1"));

        notificationService.notify(userId, NotificationType.TASK_ASSIGNED, "T", "B", "TASK", refId,
                "k");

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()),
                eq("/queue/notifications"), any(Object.class));
        verify(pushSender).push(eq(List.of("token-1")), eq("T"), eq("B"), anyMap());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2 */
    @Test
    void markRead_rejectsNotificationOfAnotherUser() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(id, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(userId, id))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode())
                                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND));
    }
}
