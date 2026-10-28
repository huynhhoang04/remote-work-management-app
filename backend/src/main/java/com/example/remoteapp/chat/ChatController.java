package com.example.remoteapp.chat;

import com.example.remoteapp.chat.dto.ConversationResponse;
import com.example.remoteapp.chat.dto.CreateDirectRequest;
import com.example.remoteapp.chat.dto.MessageResponse;
import com.example.remoteapp.chat.dto.ReadRequest;
import com.example.remoteapp.chat.dto.SendMessageRequest;
import com.example.remoteapp.common.SecurityUtils;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller chat (fallback khi WebSocket lỗi + tải lịch sử) — plan 02 §8.
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §8
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param chatService nghiệp vụ chat
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Danh sách hội thoại của người dùng.
     *
     * @return danh sách hội thoại
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> list() {
        return ResponseEntity.ok(chatService.list(SecurityUtils.currentUserId()));
    }

    /**
     * Tạo (hoặc lấy) hội thoại chat 1-1.
     *
     * @param request người muốn chat cùng
     * @return hội thoại 1-1
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> createDirect(
            @Valid @RequestBody CreateDirectRequest request) {
        return ResponseEntity.ok(chatService.createDirect(SecurityUtils.currentUserId(), request));
    }

    /**
     * Lịch sử tin nhắn (phân trang ngược).
     *
     * @param id     hội thoại
     * @param before mốc thời gian (ISO-8601, tuỳ chọn)
     * @param size   số tin mỗi trang
     * @return danh sách tin nhắn (tăng dần theo thời gian)
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageResponse>> messages(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(chatService.listMessages(id, SecurityUtils.currentUserId(),
                before, size));
    }

    /**
     * Gửi tin nhắn qua REST (fallback khi WebSocket lỗi).
     *
     * @param id      hội thoại
     * @param request nội dung + reply + clientMessageId
     * @return 201 + tin nhắn
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageResponse> send(@PathVariable UUID id,
                                                @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = chatService.send(id, SecurityUtils.currentUserId(), request);
        return ResponseEntity.created(URI.create("/api/v1/conversations/" + id + "/messages/"
                + response.id())).body(response);
    }

    /**
     * Đánh dấu đã đọc tới một tin nhắn.
     *
     * @param id      hội thoại
     * @param request tin cuối đã đọc
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §8
     */
    @PatchMapping("/conversations/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id,
                                         @Valid @RequestBody ReadRequest request) {
        chatService.markRead(id, SecurityUtils.currentUserId(), request.lastReadMessageId());
        return ResponseEntity.noContent().build();
    }
}
