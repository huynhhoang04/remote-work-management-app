# Tổng kết triển khai backend — phase 2

> Phạm vi: hoàn thiện 6 nhóm chức năng còn lại của `docs/04-thiet-ke-backend.md` §1.1
> (task, lịch cá nhân, thống kê, thông báo, chat, file) sau phase 1 (auth/user/project/invite/audit).
> Mobile chưa làm — xem `docs/plan-app/`.
>
> Trạng thái kiểm thử tại thời điểm viết:
> `./mvnw test` → **74 test xanh** (gồm integration test Testcontainers chạy Flyway V1–V6 + JPA validate);
> Bruno `bruno/run.sh --bail` → **103/103 request, 201 assertion xanh** trên backend local.

---

## 1. Trạng thái theo nhóm chức năng

| Nhóm | Chức năng | Trạng thái | Endpoint | Ghi chú |
|---|---|---|---|---|
| A | Auth, hồ sơ, thiết bị | ✅ từ phase 1 + avatar | 15 | Thêm `POST /users/me/avatar` |
| B | Dự án, thành viên, link mời, RBAC | ✅ từ phase 1 | 15 | Không đổi |
| C | Task: CRUD, gán, nhãn, checklist, comment, đính kèm, Kanban, timeline | ✅ | 26 | `start_date` + `deadline`, `position`, `@Version` |
| D | Lịch làm việc cá nhân | ✅ | 4 | Sự kiện + liên kết task |
| D | "Hôm nay", thống kê cá nhân, dashboard dự án | ✅ | 3 | Native query PostgreSQL |
| E | Trung tâm thông báo + nhắc deadline + FCM | ✅ | 4 + scheduler | FCM no-op nếu chưa có credentials |
| F | Chat GROUP theo dự án + DIRECT 1-1 + read receipt + WebSocket | ✅ | 5 REST + 2 STOMP | Broker in-memory |
| G | Audit log | ✅ từ phase 1 | 1 | `TASK_*`/`COMMENT_CREATED` đã dùng |
| H | File: upload/download/xoá, avatar | ✅ | 3 + 2 attachment | Supabase Storage hoặc đĩa local |

Tổng: **64 route controller + `/actuator/health` = 65 endpoint** (xem §3), 2 destination STOMP.

---

## 2. Bản đồ file

Đường dẫn tính từ `backend/src/main/java/com/example/remoteapp/`.

### 2.1 Nền tảng dùng chung (sửa)

| File | Loại | Vai trò |
|---|---|---|
| `common/ErrorCode.java` | enum | Thêm 20 mã lỗi cho task/schedule/file/chat/notification/stats |
| `common/GlobalExceptionHandler.java` | config | 409 optimistic lock, 400 upload quá lớn, 400 constraint violation |
| `config/SecurityConfig.java` | config | Permit `/ws`, `/ws/**` (JWT kiểm ở handshake) |
| `config/WebSocketConfig.java` | config | Broker STOMP in-memory + endpoint `/ws` |
| `user/UserService.java` | service | Avatar, list FCM token, xoá token hỏng, ghép tên nhiều user |
| `user/UserController.java` | controller | `POST /users/me/avatar` |
| `resources/application.yml` | config | `app.timezone`, `app.storage.*`, `app.fcm.*`, `app.reminder.*` |
| `pom.xml` | build | `spring-boot-starter-websocket`, `firebase-admin 9.4.3` |

### 2.2 Module `task`

| File | Loại | Vai trò |
|---|---|---|
| `Task.java` | entity | Task + timeline + Kanban + `@Version` + soft delete |
| `TaskStatus.java`, `TaskPriority.java` | enum | TODO/DOING/REVIEW/DONE; LOW/MEDIUM/HIGH/URGENT |
| `TaskAssignee.java` | entity | Gán task cho thành viên |
| `Label.java`, `TaskLabel.java` | entity | Nhãn dự án + liên kết task–nhãn |
| `TaskComment.java` | entity | Bình luận (soft delete) |
| `TaskRepository.java` | repository | Task theo dự án/parent, max position |
| `TaskAssigneeRepository.java` | repository | Assignee theo task/nhiều task, diff |
| `LabelRepository.java` | repository | Nhãn theo dự án, chống trùng tên |
| `TaskLabelRepository.java` | repository | Liên kết nhãn, đếm/xoá theo nhãn |
| `TaskCommentRepository.java` | repository | Bình luận, đếm theo nhiều task (projection) |
| `TaskService.java` | service | CRUD task, assignee, trạng thái, position, checklist, nhãn, attachment; audit + notification |
| `TaskCommentService.java` | service | Bình luận + @mention, quyền tác giả |
| `TaskController.java` | controller | Toàn bộ route task/label/subtask/comment/attachment |
| `dto/TaskRequest.java`, `dto/TaskUpdateRequest.java` | DTO | Tạo / PATCH task |
| `dto/TaskResponse.java` | DTO | Task kèm assignee/nhãn/subtask/file/số comment |
| `dto/TaskBriefResponse.java`* | DTO | (nằm trong module stats) task rút gọn cho "Hôm nay" |
| `dto/LabelRequest.java`, `dto/LabelResponse.java` | DTO | Nhãn |
| `dto/StatusRequest.java`, `dto/PositionRequest.java`, `dto/AssigneesRequest.java` | DTO | Kanban/gán |
| `dto/SubtaskRequest.java`, `dto/SubtaskUpdateRequest.java` | DTO | Checklist |
| `dto/CommentRequest.java`, `dto/CommentResponse.java` | DTO | Bình luận + mention |

\* `TaskBriefResponse` nằm ở `stats/dto` để stats không phụ thuộc DTO task.

### 2.3 Module `file`

| File | Loại | Vai trò |
|---|---|---|
| `StorageService.java` | interface | Cổng lưu trữ (đổi backend không đổi API) |
| `LocalStorageService.java` | component | Lưu `app.upload.dir` (dev/test; bật khi `STORAGE_TYPE=local`) |
| `SupabaseStorageService.java` | component | REST Supabase Storage (bật khi `STORAGE_TYPE=supabase`) |
| `FileMeta.java` | entity | Metadata: owner/project/task/message/name/mime/size/storageKey |
| `FileRepository.java` | repository | File theo task/message |
| `FileService.java` | service | Validate MIME/size, upload/download/delete, avatar, quyền |
| `FileController.java` | controller | `POST/GET/DELETE /files` |
| `dto/FileResponse.java` | DTO | Metadata + URL tải |

### 2.4 Module `schedule`

| File | Loại | Vai trò |
|---|---|---|
| `ScheduleEvent.java` | entity | Sự kiện cá nhân + `taskId?` |
| `ScheduleEventRepository.java` | repository | Theo user + khoảng thời gian |
| `ScheduleService.java` | service | CRUD, validate thời gian, ownership, validate task |
| `ScheduleController.java` | controller | `/schedule/events` |
| `dto/ScheduleEventRequest.java`, `dto/ScheduleEventResponse.java` | DTO | Request/response |

### 2.5 Module `stats`

| File | Loại | Vai trò |
|---|---|---|
| `StatsRepository.java` | repository (JdbcTemplate) | Query tổng hợp trên tasks/task_assignees/project_members/schedule_events |
| `StatsService.java` | service | "Hôm nay", thống kê cá nhân, dashboard dự án |
| `StatsController.java` | controller | `/me/today`, `/me/stats`, `/projects/{id}/stats` |
| `dto/TodayResponse.java` | DTO | 4 nhóm dữ liệu |
| `dto/TaskBriefResponse.java`, `dto/EventBriefResponse.java` | DTO | Item rút gọn |
| `dto/MyStatsResponse.java` | DTO | Chuỗi ngày/tuần/tháng + on-time rate |
| `dto/ProjectStatsResponse.java` | DTO | Tổng, theo status, % done, quá hạn, theo thành viên |

### 2.6 Module `notification`

| File | Loại | Vai trò |
|---|---|---|
| `Notification.java` | entity | Thông báo + `dedupe_key` + `read_at` |
| `NotificationType.java` | enum | TASK_ASSIGNED/MENTION/REMIND_1D/REMIND_1H/OVERDUE/MESSAGE |
| `NotificationRepository.java` | repository | Phân trang, unread count, read, chống trùng |
| `NotificationService.java` | service | Lưu DB → WS → push; dedupe |
| `NotificationController.java` | controller | `/notifications` |
| `PushSender.java` | interface | Cổng gửi push |
| `NoopPushSender.java` | component | Khi chưa cấu hình FCM (log only) |
| `FcmPushSender.java` | component | Firebase multicast, xoá token hỏng |
| `ReminderRepository.java` | repository (JdbcTemplate) | Task tới hạn/quá hạn + người nhận |
| `ReminderScheduler.java` | scheduler | Quét 5 phút: 1 ngày / 1 giờ / quá hạn (1 lần/ngày) |
| `dto/NotificationResponse.java`, `dto/UnreadCountResponse.java` | DTO | Response |

### 2.7 Module `chat`

| File | Loại | Vai trò |
|---|---|---|
| `Conversation.java` | entity | GROUP theo dự án / DIRECT 1-1 + `directKey` |
| `ConversationType.java` | enum | GROUP, DIRECT |
| `ConversationMember.java` | entity | Thành viên + `last_read_message_id` |
| `Message.java` | entity | Tin nhắn + `clientMessageId` + reply + soft delete |
| `ConversationRepository.java` | repository | Group theo dự án, direct theo key |
| `ConversationMemberRepository.java` | repository | Theo user/hội thoại |
| `MessageRepository.java` | repository | Phân trang ngược, idempotent, unread, tin cuối |
| `ChatService.java` | service | Danh sách hội thoại, direct idempotent, gửi tin, read receipt, broadcast, notify |
| `ChatController.java` | controller | REST |
| `ChatWsController.java` | controller | STOMP `/chat.send`, `/conversations.read`, lỗi `/user/queue/errors` |
| `ws/JwtHandshakeInterceptor.java` | config | Xác thực JWT khi handshake |
| `ws/StompPrincipalHandshakeHandler.java`, `ws/StompPrincipal.java` | config | Principal = userId |
| `dto/ConversationResponse.java`, `dto/CreateDirectRequest.java` | DTO | Hội thoại |
| `dto/MessageResponse.java`, `dto/SendMessageRequest.java`, `dto/WsSendMessageRequest.java` | DTO | Tin nhắn REST/WS |
| `dto/ReadRequest.java`, `dto/WsReadRequest.java`, `dto/ReadReceiptResponse.java` | DTO | Read receipt |

### 2.8 Migration

| File | Bảng mới |
|---|---|
| `V3__tasks.sql` | `tasks`, `task_assignees`, `labels`, `task_labels`, `task_comments` |
| `V4__schedule.sql` | `schedule_events` |
| `V5__chat.sql` | `conversations`, `conversation_members`, `messages` |
| `V6__notifications_files.sql` | `notifications`, `files` |

### 2.9 Bruno

| Đường dẫn | Nội dung |
|---|---|
| `bruno/collections/01-api/` | 103 request, phủ 65 route, tag theo module |
| `bruno/collections/00-smoke/` | 2 request read-only cho backend đã deploy |
| `bruno/collections/02-auth-reset/` | 1 request reset password (thủ công) |
| `bruno/environments/local.yml` | baseUrl localhost + biến input/output |
| `bruno/fixtures/` | `sample.txt`, `sample.png` cho test multipart |
| `bruno/README.md` | Coverage matrix route → request + safety notes |

---

## 3. API inventory (64 route controller + health)

Quyền theo dự án: `OWNER > ADMIN > MEMBER > VIEWER`. "Bearer" = chỉ cần đăng nhập.
Số cuối là request trong `bruno/collections/01-api`.

### Auth, user, device (15)

| Method + Path | Quyền | Request → Response | Bruno |
|---|---|---|---|
| `POST /auth/register` | Public | `{email,password,fullName}` → user | 01, 02, 23, 24, 27 |
| `POST /auth/login` | Public | `{email,password}` → token + user | 03, 04, 25, 28 |
| `POST /auth/refresh` | Public | `{refreshToken}` → token mới | 07, 103 |
| `POST /auth/logout` | Bearer | `{refreshToken}` → 204 | 102 |
| `POST /auth/forgot-password` | Public | `{email}` → 204 | 08 |
| `POST /auth/reset-password` | Public | `{token,newPassword}` → 204 | 02-auth-reset/01 |
| `GET /users/me` | Bearer | — → user | 05, 06 |
| `PATCH /users/me` | Bearer | `{fullName?,avatarUrl?}` → user | 80 |
| `POST /users/me/password` | Bearer | `{oldPassword,newPassword}` → 204 | 11 |
| `POST /users/me/avatar` | Bearer | multipart `file` → user | 81 |
| `POST /devices/token` | Bearer | `{fcmToken,platform}` → 204 | 09 |
| `DELETE /devices/token` | Bearer | `{fcmToken}` → 204 | 10 |
| `GET /actuator/health` | Public | — | 00-smoke/01 |

### Dự án, thành viên, link mời, audit (15)

| Method + Path | Quyền | Request → Response | Bruno |
|---|---|---|---|
| `GET /projects` | Bearer | — → list kèm role | 13 |
| `POST /projects` | Bearer | `{name,description?}` → project OWNER | 12, 26 |
| `GET /projects/{id}` | Member+ | — → project | 14, 101 |
| `PATCH /projects/{id}` | Owner/Admin | `{name,description?}` → project | 92, 19 |
| `DELETE /projects/{id}` | Owner | — → 204 (soft) | 100 |
| `GET /projects/{id}/members` | Member+ | — → members | 17, 93 |
| `PATCH /projects/{id}/members/{userId}` | Owner/Admin | `{role}` → 204 | 18, 29, 84 |
| `DELETE /projects/{id}/members/{userId}` | Owner/Admin (self: Member+) | — → 204 | 94 |
| `POST /projects/{id}/invites` | Owner/Admin | `{expiresAt?}` → invite | 15, 88 |
| `GET /projects/{id}/invites` | Owner/Admin | — → list active | 89 |
| `DELETE /projects/{id}/invites/{inviteId}` | Owner/Admin | — → 204 | 90 |
| `POST /projects/join` | Bearer | `{code}` → project MEMBER | 16, 22, 91, 95 |
| `GET /projects/{id}/audit-logs` | Owner | `?page&size&action` → Page | 20, 21, 96 |

### Task, nhãn, checklist, comment, attachment (26)

| Method + Path | Quyền | Request → Response | Bruno |
|---|---|---|---|
| `GET /projects/{id}/tasks` | Member+ | — → list task gốc | 33 |
| `POST /tasks` | Member+ | `TaskRequest` → task | 32, 37, 85 |
| `GET /tasks/{id}` | Member+ | — → task chi tiết | 34, 98 |
| `PATCH /tasks/{id}` | Member+ | `TaskUpdateRequest` → task | 35, 36 |
| `DELETE /tasks/{id}` | Member+ | — → 204 (soft + subtask) | 97 |
| `PATCH /tasks/{id}/status` | Member+ | `{status}` → task | 38 |
| `PATCH /tasks/{id}/position` | Member+ | `{status,position}` → 204 | 39 |
| `PATCH /tasks/{id}/assignees` | Member+ | `{assigneeIds[]}` → task | 40 |
| `GET /projects/{id}/labels` | Member+ | — → list nhãn | 31 |
| `POST /projects/{id}/labels` | Member+ | `{name,color}` → nhãn | 30 |
| `DELETE /labels/{id}` | Member+ | — → 204 | 99 |
| `POST /tasks/{id}/subtasks` | Member+ | `{title}` → subtask | 41, 43 |
| `PATCH /tasks/{id}/subtasks/{sid}` | Member+ | `{title?,done?}` → 204 | 42 |
| `DELETE /tasks/{id}/subtasks/{sid}` | Member+ | — → 204 | 83 |
| `GET /tasks/{id}/comments` | Member+ | — → list comment | 45 |
| `POST /tasks/{id}/comments` | Member+ | `{content,mentionUserIds?}` → comment | 44, 46, 86 |
| `PATCH /tasks/{id}/comments/{cid}` | Tác giả | `{content}` → 204 | 47, 48 |
| `DELETE /tasks/{id}/comments/{cid}` | Tác giả | — → 204 | 49 |
| `POST /tasks/{id}/attachments` | Member+ | multipart `file` → file | 50 |
| `DELETE /tasks/{id}/attachments/{fid}` | Uploader/Owner/Admin | — → 204 | 52 |

### Lịch, thống kê (7)

| Method + Path | Quyền | Request → Response | Bruno |
|---|---|---|---|
| `GET /schedule/events?from&to` | Bearer | — → list sự kiện | 54 |
| `POST /schedule/events` | Bearer | `{title,startTime,endTime,taskId?}` → event | 53, 56 |
| `PATCH /schedule/events/{id}` | Chủ sự kiện | các trường → event | 55 |
| `DELETE /schedule/events/{id}` | Chủ sự kiện | — → 204 | 82 |
| `GET /me/today` | Bearer | — → 4 nhóm | 57 |
| `GET /me/stats?from&to` | Bearer | — → chuỗi + on-time | 58 |
| `GET /projects/{id}/stats` | Member+ | — → dashboard | 59 |

### Thông báo, chat, file (16)

| Method + Path | Quyền | Request → Response | Bruno |
|---|---|---|---|
| `GET /notifications?page&size` | Bearer | — → Page | 60, 63 |
| `GET /notifications/unread-count` | Bearer | — → `{count}` | 61, 66 |
| `PATCH /notifications/{id}/read` | Chủ thông báo | — → 204 | 62, 64 |
| `PATCH /notifications/read-all` | Bearer | — → 204 | 65 |
| `GET /conversations` | Bearer | — → list hội thoại | 67 |
| `POST /conversations` | Bearer | `{userId}` → DIRECT (idempotent) | 70, 71 |
| `GET /conversations/{id}/messages?before&size` | Thành viên | — → list tin | 69 |
| `POST /conversations/{id}/messages` | Member+ (GROUP), thành viên (DIRECT) | `{content,replyToId?,clientMessageId?}` → tin | 68, 72, 73, 75, 87 |
| `PATCH /conversations/{id}/read` | Thành viên | `{lastReadMessageId}` → 204 | 74 |
| `POST /files?projectId?` | Bearer (+Member+ nếu có dự án) | multipart `file` → file | 76 |
| `GET /files/{id}` | Member+ dự án / Bearer (avatar) | — → stream | 51, 77, 79 |
| `DELETE /files/{id}` | Uploader/Owner/Admin | — → 204 | 78 |

### STOMP (không chạy bằng Bruno CLI)

| Hướng | Destination | Payload |
|---|---|---|
| Handshake | `/ws` (header `Authorization` hoặc `?token=`) | JWT |
| Client → Server | `/app/chat.send` | `{conversationId,content,replyToId?,clientMessageId?}` |
| Client → Server | `/app/conversations.read` | `{conversationId,lastReadMessageId}` |
| Server → Client | `/topic/conversations.{id}` | `MessageResponse` / `ReadReceiptResponse` |
| Server → Client | `/user/queue/notifications` | `NotificationResponse` |
| Server → Client | `/user/queue/errors` | `{code,message}` |

---

## 4. Dữ liệu

> ERD đầy đủ 19 bảng + catalog cột/index/FK + query mẫu: [`05-schema-co-so-du-lieu.md`](05-schema-co-so-du-lieu.md).
> Dưới đây là tóm tắt bảng mới của phase 2.

### 4.1 Bảng mới

| Bảng | Cột chính | Index/unique |
|---|---|---|
| `tasks` | project_id, parent_task_id, title, description, status, priority, start_date, deadline, position, created_by, completed_at, version, deleted_at | project, deadline, status, parent, (project,status,position) |
| `task_assignees` | task_id, user_id | UNIQUE(task_id,user_id), user |
| `labels` | project_id, name, color | UNIQUE(project_id,name) |
| `task_labels` | task_id, label_id | UNIQUE(task_id,label_id) |
| `task_comments` | task_id, user_id, content, deleted_at | (task_id,created_at) |
| `schedule_events` | user_id, title, description, start_time, end_time, task_id | (user_id,start_time) |
| `conversations` | project_id, type, direct_key | UNIQUE(direct_key), UNIQUE(project_id) WHERE type=GROUP |
| `conversation_members` | conversation_id, user_id, last_read_message_id | UNIQUE(conversation,user), user |
| `messages` | conversation_id, sender_id, content, reply_to_id, client_message_id, deleted_at | UNIQUE(conversation,client_message_id), (conversation,created_at) |
| `notifications` | user_id, type, title, body, ref_type, ref_id, dedupe_key, read_at | UNIQUE(dedupe_key) WHERE NOT NULL, (user,read_at) |
| `files` | owner_id, project_id, task_id, message_id, name, mime_type, size, storage_key | task, message, owner |

### 4.2 Quan hệ

- `tasks.project_id → projects.id`, `tasks.parent_task_id → tasks.id` (1 cấp).
- `task_comments`/`task_assignees`/`task_labels` → `tasks.id`.
- `schedule_events.task_id → tasks.id` (nullable).
- `messages.conversation_id → conversations.id`, `sender_id → users.id`.
- `files.task_id → tasks.id`, `files.message_id → messages.id` (đều nullable).
- `notifications.user_id → users.id`.

---

## 5. Cấu hình

| Biến | Mặc định | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `APP_TIMEZONE` | `Asia/Ho_Chi_Minh` | Không | Múi giờ "hôm nay", quá hạn, thống kê |
| `STORAGE_TYPE` | `local` | Không | `local` = đĩa; `supabase` = object storage |
| `STORAGE_BUCKET` | `remoteapp-uploads` | Khi supabase | Bucket |
| `STORAGE_BASE_URL` | — | Khi supabase | `https://<ref>.supabase.co` |
| `STORAGE_SERVICE_KEY` | — | Khi supabase | service_role key (secret) |
| `STORAGE_PUBLIC_URL` | — | Không | URL public của bucket (tài liệu/verify) |
| `UPLOAD_DIR` | `uploads` | Không | Thư mục khi `STORAGE_TYPE=local` |
| `FCM_ENABLED` | `false` | Không | `true` mới gửi push thật |
| `FCM_CREDENTIALS_BASE64` | — | Khi FCM bật | Service account JSON đã base64 (secret) |
| `REMINDER_INTERVAL_MS` | `300000` | Không | Chu kỳ quét nhắc deadline |
| `REMINDER_INITIAL_DELAY_MS` | `60000` | Không | Hoãn lần quét đầu |

Chi tiết setup Supabase/Firebase: `docs/plan-deploy/02-dich-vu-ngoai.md`.

---

## 6. Test

| File | Loại | Case | Luật nghiệp vụ phủ |
|---|---|---|---|
| `task/TaskServiceTest` | Mockito | 7 | deadline, parent lồng cấp, assignee ngoài dự án, version lệch, DONE↔completedAt, soft delete + subtask, notify chỉ người mới |
| `task/TaskCommentServiceTest` | Mockito | 2 | mention trừ tác giả, chỉ tác giả sửa |
| `file/FileServiceTest` | Mockito | 5 | MIME whitelist, quá size, avatar chỉ ảnh, metadata/storage, quyền xoá |
| `schedule/ScheduleServiceTest` | Mockito | 3 | start < end, ownership, validate task liên kết |
| `stats/StatsServiceTest` | Mockito | 4 | % hoàn thành, range sai, 4 nhóm "Hôm nay", chuỗi thống kê |
| `notification/NotificationServiceTest` | Mockito | 3 | dedupe, WS + push, không đọc hộ |
| `notification/ReminderSchedulerTest` | Mockito | 2 | 3 mốc nhắc, fallback người tạo |
| `chat/ChatServiceTest` | Mockito | 5 | direct idempotent, clientMessageId idempotent, broadcast/notify trừ sender, VIEWER 403, non-member 403 |
| `FeatureIntegrationTest` | Testcontainers + MockMvc | 1 luồng lớn | Flyway V1–V6, JPA validate, task→notification→stats→schedule→chat→file→RBAC→logout |
| 7 file test phase 1 | Mockito/Testcontainers | 42 | Auth/JWT/token/project/invite/rate limit + luồng auth/project |
| **Tổng** | | **74** | |

Chạy:

```bash
cd backend && ./mvnw test
```

Testcontainers tự bỏ qua nếu không có Docker; khi có Docker/Podman thì chạy integration test
(đã xác nhận xanh với Podman: `DOCKER_HOST=unix:///run/user/$UID/podman/podman.sock TESTCONTAINERS_RYUK_DISABLED=true ./mvnw test`).

---

## 7. Quyết định & deviation

| # | Quyết định | Lý do |
|---|---|---|
| 1 | Supabase Storage (REST) + `LocalStorageService` fallback | Render free không có persistent disk; không thêm SDK nặng; đổi MinIO/S3 chỉ cần implementation mới |
| 2 | FCM no-op khi thiếu credentials | Backend phải chạy được ở môi trường chưa có Firebase; notification center vẫn đủ |
| 3 | Group conversation tạo lazy | Tránh circular bean `project ↔ chat`; thành viên group lấy realtime từ project |
| 4 | `conversation_members` chỉ lưu read receipt (group) | Không phải đồng bộ khi join/remove member |
| 5 | `dedupe_key` UNIQUE cho nhắc deadline; OVERDUE 1 lần/ngày | Chống gửi trùng khi scheduler chạy lặp |
| 6 | Task `@Version` + kiểm tra version client | Chống ghi đè lạc hậu (409) |
| 7 | Generic `POST /files` chỉ nhận `projectId` | File task dùng `/tasks/{id}/attachments`; file chat chưa có endpoint riêng (xem §8) |
| 8 | `user → file`, `schedule → task` | Avatar upload và validate task liên kết (đã ghi vào `docs/plan-backend/01` §4.2) |
| 9 | Stats/reminder dùng JdbcTemplate native query | Aggregate phức tạp, đọc chéo module — ngoại lệ được plan 01 §4.2 cho phép |
| 10 | JWT không chứa role | Role đọc DB mỗi request → đổi vai trò có hiệu lực ngay |
| 11 | Không Redis/MinIO/microservice | Giữ modular monolith + broker in-memory theo docs/04 |

---

## 8. Hạn chế & việc tiếp theo

1. **Chat chưa có upload file trực tiếp trong tin nhắn**: client có thể upload qua `POST /files`
   rồi gửi URL trong `content`; chưa có `messageId` gắn tự động. Làm khi mobile cần.
2. **WebSocket chưa có integration test tự động** (Bruno CLI không chạy STOMP): đã có unit test
   service + checklist smoke tay trong `bruno/README.md`.
3. **FCM thật** cần `FCM_ENABLED=true` + service account; nếu chưa có, notification vẫn lưu DB
   nhưng không có push.
4. **Supabase bucket public** cho demo; file nhạy cảm nên chuyển bucket private + signed URL.
5. **Render free**: cold start 30–60s; 1 instance → scheduler/rate limiter in-memory đúng,
   scale ngang phải thêm Redis/DB-backed lock.
6. **Mobile app** chưa nối API — xem `docs/plan-app/01-phase-1.md`.

---

## 9. Cách chạy & kiểm thử

```bash
# 1. Hạ tầng local
docker compose -f infra/docker-compose.yml up -d       # PostgreSQL + mailpit

# 2. Backend
cd backend
./mvnw clean verify                                     # unit + integration (Testcontainers)
./mvnw spring-boot:run                                  # mặc định storage local, FCM off

# 3. Bruno (103 request, 65 route)
cd ../bruno
./run.sh --bail
./run.sh --tags tasks                                   # chạy một nhóm

# 4. Smoke trên Render
cd collections/00-smoke
npx -y @usebruno/cli run --env-file ../../environments/render.yml
```

Deploy: push `main` → GitHub Actions test + Docker build → Render deploy hook → Flyway tự chạy
`V3`–`V6` trên Neon. Xem `docs/plan-deploy/01-ci-cd-phase-1.md` và `02-dich-vu-ngoai.md`.
