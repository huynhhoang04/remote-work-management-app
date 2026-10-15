package com.example.remoteapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.remoteapp.auth.dto.LoginRequest;
import com.example.remoteapp.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test toàn luồng trên PostgreSQL thật (Testcontainers):
 * đăng ký → đăng nhập → refresh → đổi mật khẩu → dự án → link mời → RBAC → audit log.
 *
 * <p>Chạy trong CI để bắt lỗi Flyway/JPA mapping/Security filter mà unit test không thấy.
 * Tự bỏ qua khi máy không có Docker daemon (unit test vẫn chạy đủ).</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §I
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class BackendIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A1–§A4 */
    @Test
    void authFlow_registerLoginRefreshChangePassword() throws Exception {
        String email = "alice-" + UUID.randomUUID() + "@example.com";

        register(email, "password-123", "Alice");

        // đăng ký trùng email → 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, "password-123", "Alice"))))
                .andExpect(status().isConflict());

        // sai mật khẩu → 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized());

        // đăng nhập thành công
        JsonNode login = login(email, "password-123");
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();
        assertThat(accessToken).isNotBlank();

        // chưa đăng nhập → 401
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());

        // đã đăng nhập → thấy hồ sơ
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // refresh token cũ → token mới
        JsonNode refreshed = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(refreshed.get("refreshToken").asText()).isNotEqualTo(refreshToken);
        String rotatedRefresh = refreshed.get("refreshToken").asText();

        // sai mật khẩu cũ khi đổi → 400
        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong-old\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isBadRequest());

        // đổi mật khẩu thành công → 204
        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"password-123\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isNoContent());

        // đổi mật khẩu xong → mọi phiên bị thu hồi
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotatedRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        // quên mật khẩu luôn trả 204 và có gửi email
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isNoContent());
        verify(mailSender, atLeastOnce()).send(any(SimpleMailMessage.class));

        // token reset rác → 400
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"rác\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isBadRequest());
    }

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §B1–§B3, §G */
    @Test
    void projectFlow_inviteJoinRbacAndAuditLog() throws Exception {
        String ownerEmail = "owner-" + UUID.randomUUID() + "@example.com";
        String memberEmail = "member-" + UUID.randomUUID() + "@example.com";
        register(ownerEmail, "password-123", "Owner");
        register(memberEmail, "password-123", "Member");
        JsonNode owner = login(ownerEmail, "password-123");
        JsonNode member = login(memberEmail, "password-123");
        String ownerToken = owner.get("accessToken").asText();
        String memberToken = member.get("accessToken").asText();
        String memberId = member.get("user").get("id").asText();

        // tạo dự án → 201
        String projectJson = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Do an Mobile\",\"description\":\"demo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectJson).get("id").asText();

        // sinh link mời → 201 kèm code
        String inviteJson = mockMvc.perform(post("/api/v1/projects/" + projectId + "/invites")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(inviteJson).get("code").asText();
        assertThat(code).matches("[A-Z0-9]{12}");

        // thành viên join bằng mã mời → 200, vai trò MEMBER
        mockMvc.perform(post("/api/v1/projects/join")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        // join lại lần nữa → idempotent, vẫn 200
        mockMvc.perform(post("/api/v1/projects/join")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk());

        // mã mời rác → 400
        mockMvc.perform(post("/api/v1/projects/join")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NOTEXIST1234\"}"))
                .andExpect(status().isBadRequest());

        // thành viên thấy dự án trong danh sách
        mockMvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(projectId));

        // MEMBER không sửa được dự án → 403
        mockMvc.perform(patch("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hack ten\"}"))
                .andExpect(status().isForbidden());

        // Owner thấy 2 thành viên
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Owner hạ vai trò thành viên xuống VIEWER
        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isNoContent());

        // VIEWER vẫn xem được nhưng không sửa được
        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));
        mockMvc.perform(patch("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Van khong duoc\"}"))
                .andExpect(status().isForbidden());

        // chỉ Owner xem được audit log, và log đã có nội dung
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    private void register(String email, String password, String fullName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterRequest(email, password, fullName))))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
