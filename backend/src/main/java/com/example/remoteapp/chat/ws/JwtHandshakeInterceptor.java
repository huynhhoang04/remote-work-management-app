package com.example.remoteapp.chat.ws;

import com.example.remoteapp.auth.JwtService;
import com.example.remoteapp.common.AppException;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Kiểm tra JWT ở bước handshake WebSocket (header Bearer hoặc query {@code ?token=}).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    /** Khoá attribute chứa userId sau khi xác thực. */
    public static final String USER_ID_ATTRIBUTE = "userId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /**
     * Khởi tạo interceptor (constructor injection tường minh).
     *
     * @param jwtService service parse JWT
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Xác thực token trước khi nâng cấp kết nối; sai/hết hạn trả 401.
     *
     * @param request    request handshake
     * @param response   response handshake
     * @param wsHandler  handler WebSocket
     * @param attributes attributes của phiên
     * @return true nếu token hợp lệ
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            JwtService.JwtClaims claims = jwtService.parseAccessToken(token);
            attributes.put(USER_ID_ATTRIBUTE, claims.userId().toString());
            return true;
        } catch (AppException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    /**
     * Không cần dọn dẹp sau handshake.
     *
     * @param request    request handshake
     * @param response   response handshake
     * @param wsHandler  handler WebSocket
     * @param exception  exception nếu có
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // không cần xử lý
    }

    /**
     * Lấy token từ header Authorization hoặc query param {@code token}.
     *
     * @param request request handshake
     * @return token hoặc null
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("token");
    }
}
