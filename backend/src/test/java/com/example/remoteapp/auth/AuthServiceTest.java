package com.example.remoteapp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit test cho {@link AuthService} — plan 03 mục I ("unit test service cho luật nghiệp vụ chính").
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §I
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private TokenService tokenService;
    @Mock
    private LoginRateLimiter loginRateLimiter;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private static final String RAW_PASSWORD = "secret-password-1";

    private User activeUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("HASH");
        user.setFullName("Nguyen Van A");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1 */
    @Test
    void register_normalizesEmailAndEncodesPassword() {
        RegisterRequest request = new RegisterRequest("Test@Example.com", RAW_PASSWORD, "  Nguyen Van A  ");
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        UserResponse response = authService.register(request);

        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        verify(userRepository).save(any(User.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1 */
    @Test
    void register_rejectsExistingEmail() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("test@example.com", RAW_PASSWORD, "A")))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_EMAIL_EXISTS));
        verify(userRepository, never()).save(any(User.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void login_successIssuesTokens() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, "HASH")).thenReturn(true);
        JwtService.AccessToken accessToken = new JwtService.AccessToken("jwt-token", Instant.now().plusSeconds(900));
        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn(accessToken);
        when(tokenService.createRefreshToken(user.getId(), null)).thenReturn("refresh-token");

        TokenResponse response = authService.login(new LoginRequest("User@Example.com", RAW_PASSWORD));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void login_rejectsWrongPassword() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong-password")))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS));
        verify(tokenService, never()).createRefreshToken(any(), any());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void login_unknownEmailReturnsGenericError() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", RAW_PASSWORD)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void login_rejectsLockedAccount() {
        User user = activeUser();
        user.setStatus(UserStatus.LOCKED);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", RAW_PASSWORD)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_LOCKED));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void login_propagatesRateLimit() {
        doThrowRateLimited();
        assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", RAW_PASSWORD)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_RATE_LIMITED));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void refresh_rotatesTokens() {
        User user = activeUser();
        when(tokenService.rotate("old-refresh")).thenReturn(new TokenService.RefreshRotation(user.getId(), "new-refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        JwtService.AccessToken accessToken = new JwtService.AccessToken("new-jwt", Instant.now().plusSeconds(900));
        when(jwtService.generateAccessToken(user.getId(), user.getEmail())).thenReturn(accessToken);

        TokenResponse response = authService.refresh(new RefreshRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-jwt");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A2 */
    @Test
    void logout_revokesAllSessions() {
        UUID actorId = UUID.randomUUID();

        authService.logout(actorId, new RefreshRequest("refresh-token"));

        verify(tokenService).revokeRefreshToken("refresh-token");
        verify(tokenService).revokeAllForUser(actorId);
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4 */
    @Test
    void changePassword_rejectsWrongOldPassword() {
        User user = activeUser();
        UUID actorId = user.getId();
        when(userRepository.findById(actorId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-old", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(actorId,
                new ChangePasswordRequest("wrong-old", RAW_PASSWORD)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_INVALID));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4 */
    @Test
    void changePassword_revokesSessionsAndAudits() {
        User user = activeUser();
        UUID actorId = user.getId();
        when(userRepository.findById(actorId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, "HASH")).thenReturn(true);
        when(passwordEncoder.encode("new-password-1")).thenReturn("NEW_HASH");

        authService.changePassword(actorId, new ChangePasswordRequest(RAW_PASSWORD, "new-password-1"));

        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(tokenService).revokeAllForUser(actorId);
        verify(auditService).record(eq(actorId), eq((UUID) null), eq(AuditAction.USER_PASSWORD_CHANGED),
                eq("USER"), eq(actorId), isNull());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void forgotPassword_unknownEmailSendsNothing() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(new ForgotPasswordRequest("nobody@example.com"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void forgotPassword_sendsResetLink() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(tokenService.createPasswordResetToken(user.getId())).thenReturn("reset-token");

        authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void forgotPassword_mailFailureDoesNotFailRequest() {
        User user = activeUser();
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(tokenService.createPasswordResetToken(user.getId())).thenReturn("reset-token");
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void resetPassword_changesPasswordAndRevokesSessions() {
        User user = activeUser();
        when(tokenService.consumePasswordResetToken("reset-token")).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-password-1")).thenReturn("NEW_HASH");

        authService.resetPassword(new ResetPasswordRequest("reset-token", "new-password-1"));

        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(tokenService).revokeAllForUser(user.getId());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A3 */
    @Test
    void resetPassword_rejectsInvalidToken() {
        when(tokenService.consumePasswordResetToken("bad-token"))
                .thenThrow(new AppException(ErrorCode.RESET_TOKEN_INVALID));

        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest("bad-token", RAW_PASSWORD)))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESET_TOKEN_INVALID));
    }

    /** Stub rate limiter ném lỗi — dùng cho test login_propagatesRateLimit. */
    private void doThrowRateLimited() {
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.AUTH_RATE_LIMITED))
                .when(loginRateLimiter).hit(anyString());
    }
}
