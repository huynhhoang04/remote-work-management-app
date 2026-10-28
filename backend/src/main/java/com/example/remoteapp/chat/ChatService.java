package com.example.remoteapp.chat;

import com.example.remoteapp.chat.dto.ConversationResponse;
import com.example.remoteapp.chat.dto.CreateDirectRequest;
import com.example.remoteapp.chat.dto.MessageResponse;
import com.example.remoteapp.chat.dto.ReadReceiptResponse;
import com.example.remoteapp.chat.dto.SendMessageRequest;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.notification.NotificationService;
import com.example.remoteapp.notification.NotificationType;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.project.ProjectRole;
import com.example.remoteapp.project.ProjectService;
import com.example.remoteapp.project.dto.ProjectMemberResponse;
import com.example.remoteapp.project.dto.ProjectResponse;
import com.example.remoteapp.user.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ chat: hội thoại GROUP theo dự án + DIRECT 1-1, gửi tin idempotent,
 * read receipt, broadcast WebSocket và push cho người nhận (plan 03 §F1, §F2).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1, §F2
 */
@Slf4j
@Service
public class ChatService {

    private static final String TOPIC_PREFIX = "/topic/conversations.";
    private static final String REF_TYPE = "CONVERSATION";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int PREVIEW_LENGTH = 120;

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final ProjectService projectService;
    private final ProjectPermissionService permissionService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param conversationRepository repository hội thoại
     * @param memberRepository       repository thành viên hội thoại
     * @param messageRepository      repository tin nhắn
     * @param projectService         danh sách dự án/thành viên
     * @param permissionService      kiểm tra quyền theo dự án
     * @param userService            ghép tên người gửi
     * @param notificationService    thông báo tin nhắn mới
     * @param messagingTemplate      broadcast STOMP
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ChatService(ConversationRepository conversationRepository,
                       ConversationMemberRepository memberRepository,
                       MessageRepository messageRepository, ProjectService projectService,
                       ProjectPermissionService permissionService, UserService userService,
                       NotificationService notificationService,
                       SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.projectService = projectService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Danh sách hội thoại của người dùng (GROUP theo dự án + DIRECT), kèm unread + tin cuối.
     *
     * @param actorId người dùng
     * @return danh sách hội thoại, mới hoạt động trước
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @Transactional
    public List<ConversationResponse> list(UUID actorId) {
        List<ConversationResponse> result = new ArrayList<>();
        for (ProjectResponse project : projectService.listForUser(actorId)) {
            Conversation conversation = ensureGroupConversation(project.id());
            ConversationMember member = memberRepository
                    .findByConversationIdAndUserId(conversation.getId(), actorId).orElse(null);
            result.add(toConversationResponse(conversation, actorId, project.name(), member));
        }
        for (ConversationMember member : memberRepository.findByUserId(actorId)) {
            Conversation conversation = conversationRepository.findById(member.getConversationId())
                    .orElse(null);
            if (conversation == null || conversation.getType() != ConversationType.DIRECT) {
                continue;
            }
            UUID peerId = peerId(conversation.getId(), actorId);
            String name = peerId == null
                    ? "Chat"
                    : userService.namesByIds(Set.of(peerId)).getOrDefault(peerId, "Chat");
            result.add(toConversationResponse(conversation, actorId, name, member));
        }
        result.sort(Comparator.comparing(this::activity).reversed());
        return result;
    }

    /**
     * Tạo (hoặc lấy) hội thoại chat 1-1 — idempotent theo cặp user.
     *
     * @param actorId người yêu cầu
     * @param request người muốn chat cùng
     * @return hội thoại 1-1
     * @throws AppException USER_NOT_FOUND nếu người kia không tồn tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    @Transactional
    public ConversationResponse createDirect(UUID actorId, CreateDirectRequest request) {
        UUID targetId = request.userId();
        if (targetId.equals(actorId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "Không thể tạo hội thoại với chính mình");
        }
        String name = userService.namesByIds(Set.of(targetId)).get(targetId);
        if (name == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng");
        }
        String key = directKey(actorId, targetId);
        Conversation conversation = conversationRepository.findByDirectKey(key)
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setType(ConversationType.DIRECT);
                    created.setDirectKey(key);
                    return conversationRepository.save(created);
                });
        ensureMember(conversation.getId(), actorId);
        ensureMember(conversation.getId(), targetId);
        return toConversationResponse(conversation, actorId, name,
                memberRepository.findByConversationIdAndUserId(conversation.getId(), actorId)
                        .orElse(null));
    }

    /**
     * Lịch sử tin nhắn (phân trang ngược, trả về theo thứ tự thời gian tăng dần).
     *
     * @param conversationId hội thoại
     * @param actorId        người yêu cầu
     * @param before         mốc thời gian (null = mới nhất)
     * @param size           số tin mỗi trang (mặc định 20, tối đa 100)
     * @return danh sách tin nhắn
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @Transactional(readOnly = true)
    public List<MessageResponse> listMessages(UUID conversationId, UUID actorId, Instant before,
                                              Integer size) {
        Conversation conversation = findConversation(conversationId);
        requireReadable(conversation, actorId);
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(0, pageSize);
        List<Message> messages = before == null
                ? messageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        conversationId, pageable)
                : messageRepository
                        .findByConversationIdAndDeletedAtIsNullAndCreatedAtBeforeOrderByCreatedAtDesc(
                                conversationId, before, pageable);
        List<Message> chronological = new ArrayList<>(messages);
        Collections.reverse(chronological);
        Map<UUID, String> names = userService.namesByIds(chronological.stream()
                .map(Message::getSenderId).collect(Collectors.toCollection(LinkedHashSet::new)));
        return chronological.stream().map(message -> toMessage(message, names)).toList();
    }

    /**
     * Gửi tin nhắn: kiểm quyền → idempotent theo clientMessageId → lưu DB →
     * broadcast WS → thông báo cho thành viên khác.
     *
     * @param conversationId hội thoại
     * @param actorId        người gửi
     * @param request        nội dung + reply + clientMessageId
     * @return tin nhắn đã lưu
     * @throws AppException CONVERSATION_FORBIDDEN / MESSAGE_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    @Transactional
    public MessageResponse send(UUID conversationId, UUID actorId, SendMessageRequest request) {
        Conversation conversation = findConversation(conversationId);
        List<UUID> recipients = requireWritable(conversation, actorId);

        if (request.clientMessageId() != null) {
            Optional<Message> existing = messageRepository
                    .findByConversationIdAndClientMessageId(conversationId, request.clientMessageId());
            if (existing.isPresent()) {
                return toMessage(existing.get(), userService.namesByIds(
                        Set.of(existing.get().getSenderId())));
            }
        }
        if (request.replyToId() != null) {
            Message replyTo = messageRepository.findByIdAndDeletedAtIsNull(request.replyToId())
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND,
                            "Không tìm thấy tin nhắn được trả lời"));
            if (!conversationId.equals(replyTo.getConversationId())) {
                throw new AppException(ErrorCode.MESSAGE_NOT_FOUND,
                        "Tin nhắn được trả lời không thuộc hội thoại này");
            }
        }

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(actorId);
        message.setContent(request.content().trim());
        message.setReplyToId(request.replyToId());
        message.setClientMessageId(request.clientMessageId());
        messageRepository.save(message);

        MessageResponse response = toMessage(message,
                userService.namesByIds(Set.of(actorId)));
        broadcast(conversationId, response);
        notifyOthers(conversationId, recipients, actorId, response);
        return response;
    }

    /**
     * Đánh dấu đã đọc tới một tin nhắn và broadcast read receipt.
     *
     * @param conversationId    hội thoại
     * @param actorId           người đọc
     * @param lastReadMessageId tin cuối đã đọc
     * @throws AppException MESSAGE_NOT_FOUND / CONVERSATION_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    @Transactional
    public void markRead(UUID conversationId, UUID actorId, UUID lastReadMessageId) {
        Conversation conversation = findConversation(conversationId);
        requireReadable(conversation, actorId);
        Message message = messageRepository.findByIdAndDeletedAtIsNull(lastReadMessageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND,
                        "Không tìm thấy tin nhắn"));
        if (!conversationId.equals(message.getConversationId())) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND,
                    "Tin nhắn không thuộc hội thoại này");
        }
        ConversationMember member = memberRepository
                .findByConversationIdAndUserId(conversationId, actorId)
                .orElseGet(() -> newMember(conversationId, actorId));
        member.setLastReadMessageId(lastReadMessageId);
        memberRepository.save(member);
        broadcast(conversationId, new ReadReceiptResponse(conversationId, actorId,
                lastReadMessageId, Instant.now()));
    }

    /**
     * Lấy (hoặc tạo) kênh GROUP của dự án.
     *
     * @param projectId dự án
     * @return hội thoại GROUP
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private Conversation ensureGroupConversation(UUID projectId) {
        return conversationRepository.findByProjectIdAndType(projectId, ConversationType.GROUP)
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setProjectId(projectId);
                    conversation.setType(ConversationType.GROUP);
                    return conversationRepository.save(conversation);
                });
    }

    /**
     * Đảm bảo một người là thành viên hội thoại.
     *
     * @param conversationId hội thoại
     * @param userId         người dùng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private void ensureMember(UUID conversationId, UUID userId) {
        if (!memberRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            memberRepository.save(newMember(conversationId, userId));
        }
    }

    /**
     * Tạo quan hệ thành viên mới.
     *
     * @param conversationId hội thoại
     * @param userId         người dùng
     * @return entity chưa lưu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private ConversationMember newMember(UUID conversationId, UUID userId) {
        ConversationMember member = new ConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        return member;
    }

    /**
     * Kiểm tra quyền đọc hội thoại.
     *
     * @param conversation hội thoại
     * @param actorId      người yêu cầu
     * @throws AppException CONVERSATION_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private void requireReadable(Conversation conversation, UUID actorId) {
        if (conversation.getType() == ConversationType.GROUP) {
            permissionService.requireMember(conversation.getProjectId(), actorId);
            return;
        }
        if (!memberRepository.existsByConversationIdAndUserId(conversation.getId(), actorId)) {
            throw new AppException(ErrorCode.CONVERSATION_FORBIDDEN,
                    "Bạn không phải thành viên hội thoại này");
        }
    }

    /**
     * Kiểm tra quyền gửi tin và trả danh sách người nhận thông báo.
     *
     * @param conversation hội thoại
     * @param actorId      người gửi
     * @return danh sách thành viên nhận thông báo
     * @throws AppException CONVERSATION_FORBIDDEN (VIEWER không được gửi)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    private List<UUID> requireWritable(Conversation conversation, UUID actorId) {
        if (conversation.getType() == ConversationType.GROUP) {
            permissionService.requireRole(conversation.getProjectId(), actorId, ProjectRole.MEMBER);
            return projectService.listMembers(conversation.getProjectId(), actorId).stream()
                    .map(ProjectMemberResponse::userId)
                    .toList();
        }
        if (!memberRepository.existsByConversationIdAndUserId(conversation.getId(), actorId)) {
            throw new AppException(ErrorCode.CONVERSATION_FORBIDDEN,
                    "Bạn không phải thành viên hội thoại này");
        }
        return memberRepository.findByConversationId(conversation.getId()).stream()
                .map(ConversationMember::getUserId)
                .toList();
    }

    /**
     * Gửi thông báo tin nhắn mới cho các thành viên khác.
     *
     * @param conversationId hội thoại
     * @param recipients     danh sách thành viên
     * @param actorId        người gửi
     * @param message        tin nhắn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2, §E2
     */
    private void notifyOthers(UUID conversationId, List<UUID> recipients, UUID actorId,
                              MessageResponse message) {
        String title = (message.senderName().isBlank() ? "Tin nhắn mới" : message.senderName())
                + " đã gửi tin nhắn";
        String body = preview(message.content());
        for (UUID userId : recipients) {
            if (!userId.equals(actorId)) {
                notificationService.notify(userId, NotificationType.MESSAGE, title, body,
                        REF_TYPE, conversationId, null);
            }
        }
    }

    /**
     * Broadcast một sự kiện tới topic của hội thoại.
     *
     * @param conversationId hội thoại
     * @param payload        DTO sự kiện (tin nhắn / read receipt)
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    private void broadcast(UUID conversationId, Object payload) {
        try {
            messagingTemplate.convertAndSend(TOPIC_PREFIX + conversationId, payload);
        } catch (RuntimeException ex) {
            log.debug("Không broadcast được tới hội thoại {}: {}", conversationId, ex.getMessage());
        }
    }

    /**
     * Tìm hội thoại hoặc ném lỗi.
     *
     * @param conversationId id hội thoại
     * @return entity hội thoại
     * @throws AppException CONVERSATION_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private Conversation findConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND,
                        "Không tìm thấy hội thoại"));
    }

    /**
     * Tìm người chat cùng trong hội thoại 1-1.
     *
     * @param conversationId hội thoại
     * @param actorId        người dùng hiện tại
     * @return id người còn lại (null nếu không có)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private UUID peerId(UUID conversationId, UUID actorId) {
        return memberRepository.findByConversationId(conversationId).stream()
                .map(ConversationMember::getUserId)
                .filter(userId -> !userId.equals(actorId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Khoá cặp user đã sắp xếp để tạo chat 1-1 idempotent.
     *
     * @param first  user A
     * @param second user B
     * @return khoá "min_max"
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private String directKey(UUID first, UUID second) {
        String left = first.toString();
        String right = second.toString();
        return left.compareTo(right) <= 0 ? left + "_" + right : right + "_" + left;
    }

    /**
     * Dựng DTO hội thoại kèm unread + tin cuối.
     *
     * @param conversation hội thoại
     * @param actorId      người dùng hiện tại
     * @param name         tên hiển thị
     * @param member       quan hệ thành viên (có thể null)
     * @return DTO hội thoại
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    private ConversationResponse toConversationResponse(Conversation conversation, UUID actorId,
                                                        String name, ConversationMember member) {
        Message last = messageRepository
                .findFirstByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);
        long unread = unreadCount(conversation.getId(), actorId, member);
        MessageResponse lastMessage = last == null ? null : toMessage(last,
                userService.namesByIds(Set.of(last.getSenderId())));
        return new ConversationResponse(conversation.getId(), conversation.getType().name(),
                conversation.getProjectId(), name,
                member == null ? null : member.getLastReadMessageId(), unread, lastMessage,
                conversation.getCreatedAt());
    }

    /**
     * Đếm tin chưa đọc của người dùng trong hội thoại.
     *
     * @param conversationId hội thoại
     * @param userId         người dùng
     * @param member         quan hệ thành viên (có thể null)
     * @return số tin chưa đọc
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F1
     */
    private long unreadCount(UUID conversationId, UUID userId, ConversationMember member) {
        if (member == null || member.getLastReadMessageId() == null) {
            return messageRepository.countByConversationIdAndSenderIdNot(conversationId, userId);
        }
        Message lastRead = messageRepository
                .findByIdAndDeletedAtIsNull(member.getLastReadMessageId())
                .orElse(null);
        if (lastRead == null) {
            return messageRepository.countByConversationIdAndSenderIdNot(conversationId, userId);
        }
        return messageRepository.countByConversationIdAndSenderIdNotAndCreatedAtAfter(
                conversationId, userId, lastRead.getCreatedAt());
    }

    /**
     * Ánh xạ tin nhắn sang DTO (tự lấy tên người gửi).
     *
     * @param message tin nhắn
     * @param names   map tên người dùng
     * @return DTO tin nhắn
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    private MessageResponse toMessage(Message message, Map<UUID, String> names) {
        return new MessageResponse(message.getId(), message.getConversationId(),
                message.getSenderId(), names.getOrDefault(message.getSenderId(), ""),
                message.getContent(), message.getReplyToId(), message.getClientMessageId(),
                message.getCreatedAt());
    }

    /**
     * Mốc hoạt động gần nhất của hội thoại (dùng để sắp xếp danh sách).
     *
     * @param conversation DTO hội thoại
     * @return thời điểm tin cuối hoặc thời điểm tạo
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    private Instant activity(ConversationResponse conversation) {
        return conversation.lastMessage() == null
                ? conversation.createdAt()
                : conversation.lastMessage().createdAt();
    }

    /**
     * Cắt nội dung tin nhắn làm preview thông báo.
     *
     * @param content nội dung
     * @return preview ≤ 120 ký tự
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    private String preview(String content) {
        return content.length() <= PREVIEW_LENGTH
                ? content
                : content.substring(0, PREVIEW_LENGTH) + "...";
    }
}
