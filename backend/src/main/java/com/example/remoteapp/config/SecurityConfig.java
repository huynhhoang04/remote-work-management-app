package com.example.remoteapp.config;

import com.example.remoteapp.auth.JwtAuthenticationFilter;
import com.example.remoteapp.common.RestAuthenticationEntryPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình Spring Security: stateless + JWT filter + danh sách endpoint public.
 *
 * <p>Phân quyền chi tiết theo dự án (RBAC) thực hiện ở service layer qua
 * {@code ProjectPermissionService} — tại đây chỉ phân biệt đã/ chưa đăng nhập.</p>
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §5
 * @reference docs/04-thiet-ke-backend.md §9, §10
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    /**
     * Bean mã hoá mật khẩu BCrypt (cost 10).
     *
     * @return PasswordEncoder BCrypt
     * @reference docs/04-thiet-ke-backend.md §10 (NFR-S02)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * Chuỗi filter chính: vô hiệu CSRF (API stateless), session STATELESS,
     * cho phép public các endpoint auth + swagger + health, còn lại bắt buộc Bearer.
     *
     * @param http đối tượng cấu hình Spring Security
     * @return SecurityFilterChain
     * @throws Exception khi cấu hình lỗi
     * @reference docs/plan-backend/02-danh-sach-api.md §1.3
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * CORS cho Swagger UI + dev (app mobile không bị CORS ràng buộc).
     *
     * @return CorsConfigurationSource cho mọi path
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §7
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
