package com.example.remoteapp.chat.ws;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Gắn principal userId (đã xác thực ở {@link JwtHandshakeInterceptor}) vào phiên STOMP.
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 */
public class StompPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    /**
     * Tạo principal từ attribute {@code userId}.
     *
     * @param request    request handshake
     * @param wsHandler  handler WebSocket
     * @param attributes attributes của phiên
     * @return principal userId hoặc null
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userId = attributes.get(JwtHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return userId == null ? null : new StompPrincipal(userId.toString());
    }
}
