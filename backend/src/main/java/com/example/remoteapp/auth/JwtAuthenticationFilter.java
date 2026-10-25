package com.example.remoteapp.auth;

import com.example.remoteapp.common.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter trích JWT từ header {@code Authorization: Bearer} và gắn
 * {@code userId} vào SecurityContext cho request hiện tại.
 *
 * <p>Token thiếu/sai → bỏ qua (không set auth), request tới endpoint được bảo vệ
 * sẽ bị {@code RestAuthenticationEntryPoint} trả 401.</p>
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §5
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /**
     * Khởi tạo filter (constructor injection tường minh).
     *
     * @param jwtService service kiểm tra / giải mã access token
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §5
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Xác thực request bằng JWT nếu có.
     *
     * @param request     request hiện tại
     * @param response    response
     * @param filterChain chuỗi filter
     * @throws ServletException lỗi servlet
     * @throws IOException      lỗi IO
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §5
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                JwtService.JwtClaims claims = jwtService.parseAccessToken(token);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        claims.userId().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AppException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
