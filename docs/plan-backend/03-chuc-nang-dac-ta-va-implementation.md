# Plan Backend 03 — Danh sách chức năng: Đặc tả & Cách implementation

> Bộ tài liệu `docs/plan-backend/`. Mỗi chức năng gồm **Đặc tả** (luật nghiệp vụ, validation, quyền) và **Implementation** (entity, service, thuật toán).
> API chi tiết: xem `02-danh-sach-api.md`. Kiến trúc & quy tắc module: xem `01-kien-truc-va-stack.md`.
> Phân quyền (Owner/Member/Viewer): kiểm tra qua `ProjectPermissionService.requireRole(projectId, userId, minRole)` ở **mọi** method liên quan dự án.

---

## A. Tài khoản & Xác thực

### A1. Đăng ký (A01)

**Đặc tả**
- Input: `email` (hợp lệ, unique không phân biệt hoa/thường), `password` ≥ 8 ký tự, `fullName` 1–100 ký tự.
- Email đã tồn tại → `409 USER_EMAIL_EXISTS`. Tài khoản active ngay (xác thực email để sau).

**Implementation**
- `AuthService.register`: chuẩn hoá email về lowercase → check unique → `PasswordEncoder.encode` (BCrypt cost 10) → lưu `User(status=ACTIVE)`.
- Trả DTO `{id, email, fullName}` — không trả password hash ở bất kỳ đâu.

### A2. Đăng nhập & token (A02, G04)

**Đặc tả**
- Trả `accessToken` (15 phút) + `refreshToken` (30 ngày) + thông tin user.
- Sai email/mật khẩu → `401 AUTH_INVALID_CREDENTIALS` (không tiết lộ email có tồn tại hay không).
- **Refresh token xoay vòng:** mỗi lần `POST /auth/refresh` thu hồi token cũ, cấp token mới.
- Logout / đổi mật khẩu → thu hồi toàn bộ refresh token của user.
- Rate limit login: 10 lần/phút/email.

**Implementation**
- `JwtService`: ký JWT HS256 (secret từ env `JWT_SECRET`), claim `sub=userId`, `email`; parse + validate hạn.
- `JwtAuthenticationFilter` (OncePerRequestFilter): trích token từ header → set `SecurityContext`. Endpoint public: `/auth/**`, `/ws` handshake, Swagger.
- `TokenService`: sinh refresh token ngẫu nhiên 64 bytes, **lưu hash SHA-256** vào `refresh_tokens` (kèm `device_id`, `expires_at`, `revoked`); refresh hợp lệ → revoke cũ + tạo mới (rotation).
- Rate limit: bộ đếm đơn giản in-memory theo `email` (ConcurrentHashMap + cửa sổ 1 phút) — đủ cho đồ án.

### A3. Quên / đặt lại mật khẩu (A03)

**Đặc tả**
- `POST /auth/forgot-password` **luôn trả 204** (không tiết lộ email tồn tại).
- Email tồn tại → sinh token reset (hạn 30 phút, dùng 1 lần) → gửi email chứa link reset.
- `POST /auth/reset-password` `{token, newPassword}`: token hợp lệ → đổi mật khẩu, thu hồi toàn bộ refresh token, token hết hạn/sai/dùng rồi → `400 RESET_TOKEN_INVALID`.

**Implementation**
- Bảng `password_reset_tokens` (id, user_id, **token_hash**, expires_at, used_at) — thêm vào Flyway `V1__users_auth.sql`.
- `AuthService.forgotPassword`: sinh random 32 bytes → lưu hash → `JavaMailSender` gửi link `app://reset?token=<raw>`.
- `AuthService.resetPassword`: hash token nhận được → lookup → check `used_at IS NULL` và còn hạn → đổi mật khẩu + đánh dấu used + revoke refresh tokens.

### A4. Hồ sơ & avatar (A04)

**Đặc tả**
- Sửa `fullName`, avatar (ảnh ≤ 5MB, jpg/png/webp). Đổi mật khẩu cần `oldPassword`; thành công → đăng xuất mọi thiết bị (thu hồi refresh).

**Implementation**
- `UserService.updateProfile`, `changePassword`; avatar upload qua `StorageService` → trả `avatarUrl` (endpoint `GET /files/{id}`).

### A5. Device token (nền cho push)

**Đặc tả**
- Đăng ký `fcmToken` theo user (upsert), xóa khi logout hoặc khi FCM báo token không hợp lệ.

**Implementation**
- `devices` bảng: `UNIQUE(fcm_token)`; `FcmService` gửi theo danh sách device của user; response FCM `UNREGISTERED` → xóa device đó.

---

## B. Dự án & Thành viên

### B1. Dự án CRUD (B02)

**Đặc tả**
- `name` bắt buộc 1–100 ký tự, `description` ≤ 1000. Người tạo = OWNER.
- Sửa: Owner/Admin. Xóa: Owner (soft delete — ẩn với mọi thành viên, dữ liệu con giữ nguyên).
- `GET /projects` chỉ trả dự án mình là thành viên.

**Implementation**
- `ProjectService` CRUD trên `projects`; membership check qua `project_members`. Khi tạo dự án → tạo luôn **kênh chat GROUP mặc định** (gọi `ChatService.createGroupConversation`) + audit `PROJECT_CREATED`.

### B2. Phân quyền RBAC (G01)

**Đặc tả**
- 4 vai trò theo dự án: `OWNER > ADMIN > MEMBER > VIEWER`, ma trận quyền theo `../04-thiet-ke-backend.md` mục 9.
- Đổi vai trò có hiệu lực **ngay** (role lấy từ DB mỗi request, không nhồi vào JWT).
- Viewer: mọi thao tác ghi → `403 PROJECT_FORBIDDEN`.

**Implementation**
- `ProjectPermissionService.requireRole(projectId, userId, minRole)`: query `project_members` → so thứ tự enum → ném `AppException(PROJECT_FORBIDDEN)` nếu không đủ.
- Gọi ở đầu mọi method của `task`, `chat`, `project`, `file`, `stats` (module khác ủy quyền qua service này — không query trực tiếp `project_members`).

### B3. Link mời thành viên (B03)

**Đặc tả**
- Owner/Admin sinh link mời: code ngẫu nhiên 12 ký tự `[A-Z0-9]`, không bắt buộc hạn dùng.
- `POST /projects/join {code}` → thêm bản thân vào dự án với vai trò **MEMBER**; đã là thành viên → idempotent (trả project).
- Thu hồi link không ảnh hưởng thành viên đã tham gia; code đã thu hồi/hết hạn → `400 INVITE_INVALID`.

**Implementation**
- `project_invites(code UNIQUE, project_id, created_by, expires_at, revoked)`; `InviteService.create/revoke/join`.
- Join chạy trong 1 transaction: validate code → check đã là member → insert `project_members` + audit `MEMBER_JOINED`.

---

## C. Quản lý Task

### C1. Task CRUD + nhãn + checklist (B04)

**Đặc tả**
- `title` bắt buộc ≤ 200; `description` ≤ 5000; `status`: `TODO/DOING/REVIEW/DONE`; `priority`: `LOW/MEDIUM/HIGH/URGENT`.
- `startDate` ≤ `deadline` khi có cả hai (`400 TASK_DEADLINE_INVALID` nếu sai). Deadline quá khứ: **vẫn cho lưu**, client tự cảnh báo.
- **Timeline (B07):** render từ `startDate` + `deadline` — không cần API riêng.
- Nhãn (`labels`) theo dự án: name + color; gán nhiều nhãn cho task.
- **Checklist/subtask:** task con qua `parent_task_id`, chỉ có `title` + `done`; **không lồng sâu** (subtask của subtask bị từ chối).
- `status = DONE` → set `completed_at`; đổi ngược → xóa `completed_at`.
- Xóa task = soft delete (kèm subtask).

**Implementation**
- Entity `Task` với `@Version` (chống sửa đồng thời lạc hậu — client nhận 409 nếu version cũ).
- `TaskService.create/update/delete`; validation service-level cho start/deadline.
- `PATCH /tasks/{id}/position {status, position}`: `position` kiểu `double`, kéo thả = set giá trị giữa 2 task kề (hoặc `max+1000`) — client gửi giá trị mới.

### C2. Gán task (B05) + thông báo khi gán (E04 — bản tối giản)

**Đặc tả**
- Gán cho nhiều thành viên; assignee **phải là thành viên dự án**.
- Chỉ người **mới được thêm** nhận thông báo `TASK_ASSIGNED` + push (người bị gỡ không thông báo; không có thông báo đổi deadline).

**Implementation**
- `PATCH /tasks/{id}/assignees` nhận **danh sách đầy đủ** → diff với hiện tại → insert/delete `task_assignees` → với mỗi user mới: `NotificationService.notify(TASK_ASSIGNED)` + FCM.
- Ghi audit `TASK_ASSIGNED`.

### C3. Bình luận, @mention, đính kèm (B09, D07, D08)

**Đặc tả**
- Comment: content 1–2000 ký tự. **Mention:** client gửi kèm `mentionUserIds` (không parse text); người được mention phải là thành viên dự án → nhận notification `MENTION` + push.
- Sửa/xóa comment: chỉ **tác giả** (soft delete, hiển thị "đã xóa").
- Đính kèm file: ≤ 10MB; MIME cho phép: jpg, png, gif, webp, pdf, docx, xlsx, txt, zip. Gắn vào task hoặc tin nhắn.

**Implementation**
- `TaskCommentService`: lưu `TaskComment`, tạo notification cho `mentionUserIds` (trừ chính mình), audit `COMMENT_CREATED`.
- `AttachmentService` + `StorageService` (xem mục H).

---

## D. Lịch cá nhân & Thống kê

### D1. Lịch làm việc cá nhân (B01)

**Đặc tả**
- Sự kiện thuộc cá nhân: `title`, `description?`, `startTime`, `endTime` (`startTime < endTime`), `taskId?` (liên kết task để hiển thị deadline).
- Chỉ chủ sự kiện sửa/xóa; `GET /schedule/events?from=&to=` lấy theo khoảng.

**Implementation**
- `ScheduleService` CRUD trên `schedule_events`, kiểm tra chủ sở hữu (`user_id` = người gọi).

### D2. "Hôm nay" (C02)

**Đặc tả**
- Trả 4 nhóm trong 1 call: `overdueTasks` (quá hạn, chưa DONE), `dueTodayTasks`, `upcomingTasks` (7 ngày tới), `todayEvents`.
- Ngày tính theo timezone cấu hình (mặc định `Asia/Ho_Chi_Minh`).

**Implementation**
- `StatsService.getToday`: 3 query `TaskRepository` (deadline < hôm nay / hôm nay / 7 ngày tới, theo assignee) + 1 query `ScheduleEventRepository` (sự kiện hôm nay) → gộp vào `TodayDto`.

### D3. Thống kê (C01, C04, C05)

**Đặc tả**
- `GET /me/stats?from&to`: số task **mình** hoàn thành theo ngày/tuần/tháng + tỉ lệ đúng hạn (`completed_at ≤ deadline`).
- `GET /projects/{id}/stats`: tổng số task, số theo từng status, % hoàn thành, số quá hạn, thống kê theo từng thành viên (tổng/đã xong). Viewer vẫn xem được.

**Implementation**
- JPQL aggregate + `FUNCTION('date_trunc', ...)` (hoặc native query) trong `StatsRepository` — **query đọc tổng hợp riêng**, không đụng repository của `task` ngoài mục đích thống kê.
- `% hoàn thành` = `done / total` (total = 0 → trả 0). Không cache (dữ liệu nhỏ).

---

## E. Nhắc nhở & Thông báo

### E1. Nhắc deadline tự động (E01, E06)

**Đặc tả**
- Mốc **cố định**: `REMIND_1D` (trước 1 ngày), `REMIND_1H` (trước 1 giờ), `OVERDUE` (quá hạn, chưa DONE).
- **Không gửi trùng:** mỗi (task, user, mốc) chỉ 1 lần.
- Task không có assignee → nhắc **người tạo**. Task không có deadline → bỏ qua.

**Implementation**
- `ReminderScheduler` (`@Scheduled(fixedDelay = 5 phút)`):
  1. Query task có `deadline ∈ [now+1d, now+1d+5p]`, chưa DONE → với mỗi assignee: tạo notification `REMIND_1D` **nếu chưa có** (check `exists` trước khi insert; ràng buộc `UNIQUE(task_id, user_id, type)` trong `notifications` là chốt chặn cuối).
  2. Tương tự với `REMIND_1D` → `REMIND_1H` với cửa sổ `[now+1h, now+1h+5p]`.
  3. Task `deadline < now`, `status != DONE`, chưa có `OVERDUE` → tạo `OVERDUE`.
- Mỗi notification tạo xong → `FcmService.push`. Gửi FCM lỗi → notification vẫn nằm trong trung tâm thông báo.

### E2. Trung tâm thông báo + push (E02, E03)

**Đặc tả**
- Loại thông báo: `TASK_ASSIGNED`, `MENTION`, `REMIND_1D`, `REMIND_1H`, `OVERDUE`, `MESSAGE`.
- List (phân trang), `unread-count`, đánh dấu đã đọc 1 cái / tất cả.
- Push FCM tiếng Việt, bấm vào deep-link tới task/hội thoại (payload `refType`, `refId`).

**Implementation**
- `NotificationService.notify(userId, type, title, body, refType, refId)`: insert `notifications` (kiểm tra unique cho loại nhắc) → push qua `FcmService` đến mọi device của user.
- `FcmService`: firebase-admin, gửi multicast; token hỏng → xóa device.

---

## F. Chat (D02, D03, D04)

### F1. Hội thoại

**Đặc tả**
- `GROUP`: 1 kênh/1 dự án, tự tạo khi tạo dự án; thành viên = thành viên dự án (tự động đồng bộ khi thêm/xóa thành viên dự án).
- `DIRECT`: 1-1 theo cặp user, **idempotent** (gọi lại vẫn trả cùng 1 hội thoại).
- Mỗi hội thoại có `last_read_message_id` theo từng thành viên → **unreadCount** = số tin sau mốc đó (trừ tin của mình).

**Implementation**
- `conversations(project_id, type)` + `conversation_members(last_read_message_id)`.
- `ChatService.createDirectConversation(userA, userB)`: `UNIQUE` theo khoá cặp user đã sắp xếp (`direct_key = minId + "_" + maxId`) — tránh tạo trùng.

### F2. Gửi/nhận tin nhắn (realtime)

**Đặc tả**
- Nội dung ≤ 4000 ký tự, hỗ trợ `replyToId`; `clientMessageId` (UUID client sinh) → **idempotent**: gửi lại (retry) không tạo tin trùng, trả về tin cũ.
- Không typing, không trạng thái online/offline (đã cắt).
- Thành viên khác nhận qua WebSocket khi app mở; nhận **FCM push** (loại `MESSAGE`) khi app nền/offline.

**Implementation**
- **WS chính, REST là fallback:** `ChatWsController` (`/app/chat.send`) → `ChatService.sendMessage`:
  1. Check membership (không phải thành viên → `403 CONVERSATION_FORBIDDEN`).
  2. Nếu `clientMessageId` đã tồn tại → trả message cũ (idempotent).
  3. Lưu `messages` → broadcast `/topic/conversations.{id}` qua `SimpMessagingTemplate`.
  4. `NotificationService.notify(MESSAGE)` + FCM cho mọi thành viên **trừ người gửi** (không kiểm tra online — đơn giản, có thể tối ưu sau).
- `WebSocketConfig`: broker in-memory, endpoint `/ws` với `HandshakeInterceptor` kiểm tra JWT (query param `?token=` cho client không set được header).
- **Read receipt:** `/app/conversations.read` hoặc `PATCH /conversations/{id}/read` → cập nhật `last_read_message_id` → broadcast `ReadReceiptDto` lên topic.

---

## G. Audit log (G05)

**Đặc tả**
- Ghi các hành động: `PROJECT_CREATED/UPDATED/DELETED`, `MEMBER_JOINED/REMOVED/ROLE_CHANGED`, `INVITE_CREATED/REVOKED`, `TASK_CREATED/UPDATED/DELETED/ASSIGNED/STATUS_CHANGED`, `COMMENT_CREATED`, `USER_PASSWORD_CHANGED`.
- Owner xem được log của dự án: phân trang, lọc theo `action`; vai trò khác → 403.

**Implementation**
- `AuditService.record(actorId, action, entityType, entityId, projectId, metadata)`: insert `audit_logs` **đồng bộ** trong transaction của request (bảng nhỏ, đơn giản; nếu chậm mới chuyển `@Async`).
- Gọi tại các service tương ứng; `AuditLogController` chỉ đọc.

---

## H. File (đính kèm)

**Đặc tả**
- Upload multipart ≤ 10MB, MIME whitelist (jpg, png, gif, webp, pdf, docx, xlsx, txt, zip) → `400 FILE_TOO_LARGE / FILE_TYPE_NOT_ALLOWED` nếu sai.
- Gắn vào task hoặc tin nhắn; tải về qua `GET /files/{id}` (stream kèm Content-Type).
- Quyền tải: thành viên dự án chứa file (file cá nhân như avatar: mọi người đã đăng nhập).

**Implementation**
- Interface `StorageService` + `LocalStorageService` (lưu `uploads/{uuid}.{ext}`, env `UPLOAD_DIR`) — mai sau thay `S3StorageService` mà không đổi API.
- `files` row: `owner_id, project_id, task_id, message_id, name, mime_type, size, storage_key`.
- `FileController.download`: `FileSystemResource` + header `Content-Disposition`.

---

## I. Thứ tự triển khai gợi ý (theo plan)

| Bước | Chức năng | Ghi chú |
|---|---|---|
| 1 | Dự án Maven + Flyway `V1` + Security config + Auth (A1–A3) | Chạy được `GET /users/me` |
| 2 | User/Device (A4–A5) + Project + RBAC + Link mời (B1–B3) | Có audit cơ bản |
| 3 | Task (C1–C3) + File (H) | Lõi nghiệp vụ |
| 4 | Lịch + Hôm nay + Thống kê (D1–D3) | |
| 5 | Notification + Scheduler (E1–E2) | FCM, nhắc deadline |
| 6 | Chat WS (F1–F2) + Audit đầy đủ (G) | |

**Kiểm thử:** mỗi service viết unit test (Mockito) cho luật nghiệp vụ chính (phân quyền, validate deadline, idempotent chat, nhắc không trùng); integration test repository bằng Testcontainers PostgreSQL cho query thống kê.
