package com.example.remoteapp.auth;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý vòng đời token: refresh token (xoay vòng, thu hồi) và
 * password reset token (một lần, có hạn).
 *
 * <p>Chỉ lưu **hash SHA-256** của token trong DB — token gốc chỉ nằm ở client
 * hoặc trong email (plan 03 §A2, §A3).</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2, §A3
 * @reference docs/04-thiet-ke-backend.md §10
 */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int RAW_TOKEN_BYTES = 48;

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long refreshDays;
    private final long resetMinutes;

    /**
     * Khởi tạo service với repository + hạn token từ cấu hình.
     *
     * @param refreshTokenRepository       repository refresh token
     * @param passwordResetTokenRepository repository token đặt lại mật khẩu
     * @param refreshDays                  hạn refresh token (ngày)
     * @param resetMinutes                 hạn token đặt lại mật khẩu (phút)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §7
     */
    public TokenService(RefreshTokenRepository refreshTokenRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        @Value("${app.jwt.refresh-token-days}") long refreshDays,
                        @Value("${app.auth.reset-token-minutes}") long resetMinutes) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshDays = refreshDays;
        this.resetMinutes = resetMinutes;
    }

    /**
     * Kết quả xoay vòng refresh token.
     *
     * @param userId       chủ sở hữu token
     * @param newRawToken  refresh token mới (bản gốc gửi cho client)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    public record RefreshRotation(UUID userId, String newRawToken) {
    }

    /**
     * Tạo refresh token mới, lưu hash vào DB.
     *
     * @param userId   chủ sở hữu
     * @param deviceId mã thiết bị (tuỳ chọn, có thể null)
     * @return refresh token bản gốc (client tự lưu an toàn)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional
    public String createRefreshToken(UUID userId, String deviceId) {
        String rawToken = generateRawToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(sha256Hex(rawToken));
        token.setDeviceId(deviceId);
        token.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        token.setRevoked(false);
        refreshTokenRepository.save(token);
        return rawToken;
    }

    /**
     * Xoay vòng refresh token: thu hồi token cũ, cấp token mới cho cùng user/thiết bị.
     *
     * @param rawToken refresh token client gửi lên
     * @return userId + refresh token mới
     * @throws AppException AUTH_REFRESH_INVALID nếu token sai/thu hồi/hết hạn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional
    public RefreshRotation rotate(String rawToken) {
        RefreshToken token = validateRefreshToken(rawToken);
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return new RefreshRotation(token.getUserId(), createRefreshToken(token.getUserId(), token.getDeviceId()));
    }

    /**
     * Kiểm tra refresh token hợp lệ (đúng hash, chưa thu hồi, chưa hết hạn).
     *
     * @param rawToken refresh token bản gốc
     * @return entity {@link RefreshToken} tương ứng
     * @throws AppException AUTH_REFRESH_INVALID nếu không hợp lệ
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_REFRESH_INVALID, "Refresh token không hợp lệ"));
        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.AUTH_REFRESH_INVALID, "Refresh token đã hết hạn hoặc bị thu hồi");
        }
        return token;
    }

    /**
     * Thu hồi một refresh token cụ thể (idempotent — không lỗi nếu đã thu hồi).
     *
     * @param rawToken refresh token bản gốc
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Thu hồi toàn bộ refresh token của người dùng
     * (logout / đổi mật khẩu / đặt lại mật khẩu).
     *
     * @param userId chủ sở hữu token
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2, §A3
     */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Tạo token đặt lại mật khẩu (một lần, hạn {@code app.auth.reset-token-minutes}).
     *
     * @param userId người dùng yêu cầu
     * @return token bản gốc (gửi qua email)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
     */
    @Transactional
    public String createPasswordResetToken(UUID userId) {
        String rawToken = generateRawToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(userId);
        token.setTokenHash(sha256Hex(rawToken));
        token.setExpiresAt(Instant.now().plus(resetMinutes, ChronoUnit.MINUTES));
        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    /**
     * Đánh dấu token đặt lại mật khẩu đã dùng và trả về chủ sở hữu.
     *
     * @param rawToken token trong link email
     * @return userId sở hữu token
     * @throws AppException RESET_TOKEN_INVALID nếu token sai/hết hạn/đã dùng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
     */
    @Transactional
    public UUID consumePasswordResetToken(String rawToken) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_INVALID, "Link đặt lại mật khẩu không hợp lệ"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID, "Link đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng");
        }
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        return token.getUserId();
    }

    /**
     * Sinh token ngẫu nhiên 48 bytes, mã hoá Base64URL.
     *
     * @return chuỗi token bản gốc
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    private String generateRawToken() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Tính hash SHA-256 (hex) của token — giá trị lưu trong DB.
     *
     * @param rawToken token bản gốc
     * @return hash hex 64 ký tự
     * @reference docs/04-thiet-ke-backend.md §10
     */
    private String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }
}
