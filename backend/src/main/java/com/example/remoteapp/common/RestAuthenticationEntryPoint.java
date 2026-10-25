package com.example.remoteapp.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Trả lỗi 401 theo format chuẩn khi request thiếu/ sai JWT
 * (thay cho redirect mặc định của Spring Security).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §1.1
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * Khởi tạo entry point (constructor injection tường minh).
     *
     * @param objectMapper mapper dùng để ghi body JSON
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Ghi response 401 JSON khi chưa xác thực.
     *
     * @param request       request hiện tại
     * @param response      response sẽ ghi
     * @param authException exception xác thực
     * @throws IOException khi ghi response lỗi
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ErrorResponse body = ErrorResponse.of(ErrorCode.AUTH_UNAUTHORIZED, "Chưa đăng nhập hoặc token không hợp lệ",
                request.getRequestURI());
        response.setStatus(body.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
