package com.example.remoteapp.auth;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Phát hành và kiểm tra JWT access token (HS256).
 *
 * <p>Access token ngắn hạn (mặc định 15 phút); refresh token dài hạn do
 * {@link TokenService} quản lý — không lưu JWT trong DB.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
 * @reference docs/04-thiet-ke-backend.md §10
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenMinutes;

    /**
     * Khởi tạo service với secret và hạn access token từ cấu hình.
     *
     * @param secret             chuỗi bí mật ký JWT (≥ 32 ký tự, lấy từ env)
     * @param accessTokenMinutes hạn access token (phút)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §7
     */
    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    /**
     * Access token đã phát hành kèm thời điểm hết hạn.
     *
     * @param value     chuỗi JWT
     * @param expiresAt thời điểm hết hạn
     * @reference docs/plan-backend/02-danh-sach-api.md §2
     */
    public record AccessToken(String value, Instant expiresAt) {
    }

    /**
     * Claims lấy ra từ access token hợp lệ.
     *
     * @param userId người dùng sở hữu token
     * @param email  email (tiện cho logging/audit)
     * @reference docs/plan-backend/02-danh-sach-api.md §1.3
     */
    public record JwtClaims(UUID userId, String email) {
    }

    /**
     * Sinh access token JWT cho người dùng.
     *
     * @param userId người dùng
     * @param email  email người dùng (claim phụ)
     * @return access token + thời điểm hết hạn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    public AccessToken generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(accessTokenMinutes * 60);
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
        return new AccessToken(token, expiresAt);
    }

    /**
     * Kiểm tra và giải mã access token.
     *
     * @param token chuỗi JWT từ header {@code Authorization: Bearer}
     * @return claims của token
     * @throws AppException AUTH_TOKEN_EXPIRED nếu token hết hạn,
     *                      AUTH_TOKEN_INVALID nếu token sai/chữ ký không khớp
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    public JwtClaims parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtClaims(UUID.fromString(claims.getSubject()), claims.get("email", String.class));
        } catch (ExpiredJwtException ex) {
            throw new AppException(ErrorCode.AUTH_TOKEN_EXPIRED, "Access token đã hết hạn");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new AppException(ErrorCode.AUTH_TOKEN_INVALID, "Access token không hợp lệ");
        }
    }
}
