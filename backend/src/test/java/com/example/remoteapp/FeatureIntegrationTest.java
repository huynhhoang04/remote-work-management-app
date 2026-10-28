package com.example.remoteapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.remoteapp.auth.dto.LoginRequest;
import com.example.remoteapp.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test end-to-end cho các module mới trên PostgreSQL thật (Testcontainers):
 * task → gán/thông báo → stats → schedule → chat → file → RBAC → logout.
 *
 * <p>Bắt lỗi Flyway V3–V6, JPA mapping, native query và luồng phân quyền mà unit test
 * không thấy. Tự bỏ qua khi máy không có Docker.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §I
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class FeatureIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.storage.type", () -> "local");
        registry.add("app.upload.dir",
                () -> Path.of(System.getProperty("java.io.tmpdir"), "remoteapp-it-uploads").toString());
        // Không để scheduler chạy trong test
        registry.add("app.reminder.initial-delay-ms", () -> "3600000");
    }

    @MockBean
    private JavaMailSender mailSender;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    /** @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §C–§F */
    @Test
    void fullFeatureFlow_taskStatsScheduleChatFileRbac() throws Exception {
        String ownerEmail = "owner-" + UUID.randomUUID() + "@example.com";
        String memberEmail = "member-" + UUID.randomUUID() + "@example.com";
        register(ownerEmail, "password-123", "Owner");
        register(memberEmail, "password-123", "Member");
        JsonNode owner = login(ownerEmail, "password-123");
        JsonNode member = login(memberEmail, "password-123");
        String ownerToken = owner.get("accessToken").asText();
        String memberToken = member.get("accessToken").asText();
        String memberRefresh = member.get("refreshToken").asText();
        String memberId = member.get("user").get("id").asText();

        String projectId = createProject(ownerToken, "Do an Mobile");
        String code = createInvite(ownerToken, projectId);
        mockMvc.perform(post("/api/v1/projects/join")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk());

        // --- Nhãn + task + gán người ---
        String labelJson = mockMvc.perform(post("/api/v1/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bug\",\"color\":\"#ff0000\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String labelId = objectMapper.readTree(labelJson).get("id").asText();

        Instant deadline = Instant.now().plus(1, ChronoUnit.DAYS);
        String taskJson = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + projectId + "\",\"title\":\"Lam bao cao\","
                                + "\"description\":\"mo ta\",\"status\":\"TODO\",\"priority\":\"HIGH\","
                                + "\"deadline\":\"" + deadline + "\","
                                + "\"labelIds\":[\"" + labelId + "\"],"
                                + "\"assigneeIds\":[\"" + memberId + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.assignees.length()").value(1))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(taskJson).get("id").asText();

        mockMvc.perform(get("/api/v1/projects/" + projectId + "/tasks")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taskId));

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels[0].name").value("Bug"));

        // version lạc hậu → 409
        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ten moi\",\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Ten cu\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TASK_VERSION_CONFLICT"));

        // deadline sai
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + projectId + "\",\"title\":\"Sai\","
                                + "\"startDate\":\"2026-11-02T00:00:00Z\","
                                + "\"deadline\":\"2026-11-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TASK_DEADLINE_INVALID"));

        // --- Checklist ---
        String subtaskJson = mockMvc.perform(post("/api/v1/tasks/" + taskId + "/subtasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Checklist 1\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String subtaskId = objectMapper.readTree(subtaskJson).get("id").asText();
        mockMvc.perform(post("/api/v1/tasks/" + subtaskId + "/subtasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Long cap 2\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TASK_PARENT_INVALID"));

        // --- Bình luận + mention ---
        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"@Member xem giup\",\"mentionUserIds\":[\""
                                + memberId + "\"]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/tasks/" + taskId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("@Member xem giup"));

        // --- Thông báo của member: TASK_ASSIGNED + MENTION ---
        mockMvc.perform(get("/api/v1/notifications?page=0&size=20")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type=='TASK_ASSIGNED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.type=='MENTION')]").isNotEmpty());
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(jsonPath("$.count").value(0));

        // --- Lịch cá nhân ---
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        String eventJson = mockMvc.perform(post("/api/v1/schedule/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hop nhom\",\"startTime\":\"" + start + "\","
                                + "\"endTime\":\"" + start.plus(1, ChronoUnit.HOURS) + "\","
                                + "\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(eventJson).get("id").asText();
        mockMvc.perform(get("/api/v1/schedule/events")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(eventId));

        // --- Hoàn thành task + checklist rồi xem stats ---
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/subtasks/" + subtaskId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/stats")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(2))
                .andExpect(jsonPath("$.percentDone").value(100.0))
                .andExpect(jsonPath("$.perMember.length()").value(2));
        mockMvc.perform(get("/api/v1/me/today")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayEvents").isArray());
        mockMvc.perform(get("/api/v1/me/stats")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedPerDay").isArray());

        // --- Chat ---
        String conversationsJson = mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GROUP"))
                .andReturn().getResponse().getContentAsString();
        String conversationId = objectMapper.readTree(conversationsJson).get(0).get("id").asText();

        String messageJson = mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Chao ca nha\",\"clientMessageId\":\""
                                + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String messageId = objectMapper.readTree(messageJson).get("id").asText();

        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(messageId));

        String directOne = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andReturn().getResponse().getContentAsString();
        String directTwo = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + memberId + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(directTwo).get("id").asText())
                .isEqualTo(objectMapper.readTree(directOne).get("id").asText());

        mockMvc.perform(patch("/api/v1/conversations/" + conversationId + "/read")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastReadMessageId\":\"" + messageId + "\"}"))
                .andExpect(status().isNoContent());

        // --- File đính kèm ---
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes());
        String fileJson = mockMvc.perform(multipart("/api/v1/tasks/" + taskId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.size").value(5))
                .andReturn().getResponse().getContentAsString();
        String fileId = objectMapper.readTree(fileJson).get("id").asText();

        mockMvc.perform(get("/api/v1/files/" + fileId)
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
        mockMvc.perform(get("/api/v1/files/" + fileId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/tasks/" + taskId + "/attachments/" + fileId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/files/" + fileId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());

        // --- Hồ sơ + avatar ---
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Owner Moi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Owner Moi"));
        MockMultipartFile avatar = new MockMultipartFile("file", "avatar.png", "image/png",
                "png-bytes".getBytes());
        mockMvc.perform(multipart("/api/v1/users/me/avatar")
                        .file(avatar)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers
                        .startsWith("/api/v1/files/")));

        // --- VIEWER không được ghi ---
        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/members/" + memberId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + projectId + "\",\"title\":\"Khong duoc\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PROJECT_FORBIDDEN"));
        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Viewer gui\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/stats")
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isOk());

        // --- Logout thu hồi refresh token ---
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + memberRefresh + "\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + memberRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email, String password, String fullName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password, fullName))))
                .andExpect(status().isCreated());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createProject(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"description\":\"demo\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createInvite(String token, String projectId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/projects/" + projectId + "/invites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("code").asText();
    }
}
