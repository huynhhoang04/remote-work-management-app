package com.example.remoteapp.auth;

import com.example.remoteapp.audit.AuditAction;
import com.example.remoteapp.audit.AuditService;
import com.example.remoteapp.auth.dto.ChangePasswordRequest;
import com.example.remoteapp.auth.dto.ForgotPasswordRequest;
import com.example.remoteapp.auth.dto.LoginRequest;
import com.example.remoteapp.auth.dto.RefreshRequest;
import com.example.remoteapp.auth.dto.RegisterRequest;
import com.example.remoteapp.auth.dto.ResetPasswordRequest;
import com.example.remoteapp.auth.dto.TokenResponse;
import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.user.User;
import com.example.remoteapp.user.UserRepository;
import com.example.remoteapp.user.UserStatus;
import com.example.remoteapp.user.dto.UserResponse;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nghiệp vụ xác thực: đăng ký, đăng nhập, refresh/logout, quên & đổi mật khẩu
 * (plan 03 mục A).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1–§A4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String RESET_LINK_FORMAT = "app://reset?token=%s";
    private static final String GENERIC_LOGIN_ERROR = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final LoginRateLimiter loginRateLimiter;
    private final JavaMailSender mailSender;
    private final AuditService auditService;

    /**
     * Đăng ký tài khoản mới (FR-A01).
     *
     * <p>Chuẩn hoá email về lowercase, kiểm tra trùng, hash BCrypt mật khẩu.
     * Tài khoản active ngay (xác thực email để sau).</p>
     *
     * @param request thông tin đăng ký (đã validate ở DTO)
     * @return hồ sơ người dùng mới
     * @throws AppException USER_EMAIL_EXISTS nếu email đã được đăng ký
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.USER_EMAIL_EXISTS, "Email đã được sử dụng");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        return UserResponse.from(user);
    }

    /**
     * Đăng nhập (FR-A02): kiểm tra thông tin, phát hành access + refresh token.
     *
     * @param request email + mật khẩu
     * @return access token, refresh token và hồ sơ người dùng
     * @throws AppException AUTH_INVALID_CREDENTIALS nếu sai thông tin,
     *                      USER_LOCKED nếu tài khoản bị khoá,
     *                      AUTH_RATE_LIMITED nếu vượt rate limit
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        loginRateLimiter.hit(email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, GENERIC_LOGIN_ERROR));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.USER_LOCKED, "Tài khoản đã bị khoá, vui lòng liên hệ quản trị viên");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, GENERIC_LOGIN_ERROR);
        }
        return issueTokens(user);
    }

    /**
     * Làm mới phiên đăng nhập — xoay vòng refresh token (FR-G04).
     *
     * @param request refresh token hiện tại
     * @return access token mới + refresh token mới
     * @throws AppException AUTH_REFRESH_INVALID nếu refresh token không hợp lệ
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        TokenService.RefreshRotation rotation = tokenService.rotate(request.refreshToken());
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
        JwtService.AccessToken accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return new TokenResponse(accessToken.value(), rotation.newRawToken(), accessToken.expiresAt(),
                UserResponse.from(user));
    }

    /**
     * Đăng xuất: thu hồi refresh token gửi lên + toàn bộ phiên của người dùng.
     *
     * @param actorId người dùng đăng xuất
     * @param request refresh token hiện tại
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    @Transactional
    public void logout(UUID actorId, RefreshRequest request) {
        tokenService.revokeRefreshToken(request.refreshToken());
        tokenService.revokeAllForUser(actorId);
    }

    /**
     * Đổi mật khẩu khi đã đăng nhập (FR-A04) — thu hồi toàn bộ phiên sau khi đổi.
     *
     * @param actorId người dùng đổi mật khẩu
     * @param request mật khẩu hiện tại + mật khẩu mới
     * @throws AppException PASSWORD_INVALID nếu mật khẩu hiện tại không đúng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4
     */
    @Transactional
    public void changePassword(UUID actorId, ChangePasswordRequest request) {
        User user = userRepository.findById(actorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_INVALID, "Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        tokenService.revokeAllForUser(actorId);
        auditService.record(actorId, null, AuditAction.USER_PASSWORD_CHANGED, "USER", actorId, null);
    }

    /**
     * Quên mật khẩu (FR-A03) — **luôn** thành công im lặng; email tồn tại mới gửi link.
     *
     * @param request email cần khôi phục
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .ifPresent(this::sendResetEmail);
    }

    /**
     * Đặt lại mật khẩu bằng token trong email (FR-A03).
     *
     * <p>Token dùng một lần; sau khi đổi thành công mọi phiên đăng nhập bị thu hồi.</p>
     *
     * @param request token + mật khẩu mới
     * @throws AppException RESET_TOKEN_INVALID nếu token sai/hết hạn/đã dùng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UUID userId = tokenService.consumePasswordResetToken(request.token());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        tokenService.revokeAllForUser(userId);
        auditService.record(userId, null, AuditAction.USER_PASSWORD_CHANGED, "USER", userId, "reset-password");
    }

    /**
     * Gửi email chứa link đặt lại mật khẩu (lỗi SMTP không làm fail request).
     *
     * @param user người dùng yêu cầu khôi phục
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3
     */
    private void sendResetEmail(User user) {
        String rawToken = tokenService.createPasswordResetToken(user.getId());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Đặt lại mật khẩu");
        message.setText("Nhấn vào link sau để đặt lại mật khẩu (hiệu lực 30 phút):\n"
                + RESET_LINK_FORMAT.formatted(rawToken));
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("Không gửi được email đặt lại mật khẩu cho {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    /**
     * Phát hành access + refresh token cho người dùng.
     *
     * @param user người dùng đã xác thực
     * @return response chứa token
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2
     */
    private TokenResponse issueTokens(User user) {
        JwtService.AccessToken accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = tokenService.createRefreshToken(user.getId(), null);
        return new TokenResponse(accessToken.value(), refreshToken, accessToken.expiresAt(), UserResponse.from(user));
    }

    /**
     * Chuẩn hoá email về lowercase + bỏ khoảng trắng thừa.
     *
     * @param email email từ request
     * @return email đã chuẩn hoá
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
