package com.example.remoteapp.config;

import com.example.remoteapp.chat.ws.JwtHandshakeInterceptor;
import com.example.remoteapp.chat.ws.StompPrincipalHandshakeHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket STOMP cho chat + thông báo realtime (broker in-memory).
 *
 * <p>Không dùng Redis/SockJS: một instance Render free là đủ cho đồ án
 * (docs/04 §7).</p>
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §12
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §F2
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    /**
     * Khởi tạo cấu hình (constructor injection tường minh).
     *
     * @param jwtHandshakeInterceptor interceptor xác thực JWT khi handshake
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    /**
     * Đăng ký endpoint {@code /ws} với xác thực JWT.
     *
     * @param registry registry endpoint STOMP
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor)
                .setHandshakeHandler(new StompPrincipalHandshakeHandler());
    }

    /**
     * Broker in-memory: {@code /topic} (hội thoại) + {@code /queue} (thông báo cá nhân).
     *
     * @param registry registry broker
     * @reference docs/plan-backend/02-danh-sach-api.md §12
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}
