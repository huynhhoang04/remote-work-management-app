package com.example.remoteapp.chat;

import com.example.remoteapp.chat.dto.SendMessageRequest;
import com.example.remoteapp.chat.dto.WsReadRequest;
import com.example.remoteapp.chat.dto.WsSendMessageRequest;
import com.example.remoteapp.common.AppException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * STOMP controller cho chat realtime — gửi tin và read receipt (plan 02 §12).
 *
 * <p>Broadcast do {@link ChatService} thực hiện để REST và WS dùng chung một luồng.</p>
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 */
@Controller
public class ChatWsController {

    private static final String QUEUE_ERRORS = "/queue/errors";

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param chatService       nghiệp vụ chat
     * @param messagingTemplate gửi lỗi về queue riêng của người dùng
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ChatWsController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Nhận tin nhắn từ {@code /app/chat.send}.
     *
     * @param request   payload tin nhắn
     * @param principal phiên đã xác thực (tên = userId)
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @MessageMapping("/chat.send")
    public void send(@Payload WsSendMessageRequest request, Principal principal) {
        if (principal == null) {
            return;
        }
        chatService.send(request.conversationId(), UUID.fromString(principal.getName()),
                new SendMessageRequest(request.content(), request.replyToId(),
                        request.clientMessageId()));
    }

    /**
     * Nhận read receipt từ {@code /app/conversations.read}.
     *
     * @param request   payload đánh dấu đã đọc
     * @param principal phiên đã xác thực (tên = userId)
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @MessageMapping("/conversations.read")
    public void read(@Payload WsReadRequest request, Principal principal) {
        if (principal == null) {
            return;
        }
        chatService.markRead(request.conversationId(), UUID.fromString(principal.getName()),
                request.lastReadMessageId());
    }

    /**
     * Trả lỗi nghiệp vụ về queue riêng của phiên (không đóng kết nối).
     *
     * @param ex        exception nghiệp vụ
     * @param principal phiên đã xác thực
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @MessageExceptionHandler(AppException.class)
    public void handleAppException(AppException ex, Principal principal) {
        if (principal == null) {
            return;
        }
        messagingTemplate.convertAndSendToUser(principal.getName(), QUEUE_ERRORS,
                Map.of("code", ex.getErrorCode().name(), "message", ex.getMessage()));
    }
}
