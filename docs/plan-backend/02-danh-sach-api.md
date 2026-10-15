# Plan Backend 02 — Danh sách API

> Bộ tài liệu `docs/plan-backend/`. Xem kiến trúc & quy ước tại `01-kien-truc-va-stack.md`.
> Base path: `/api/v1` · Xác thực: `Authorization: Bearer <accessToken>`.
> **Không** sort/filter phía server cho task (client tự xử lý) — xem `../04-thiet-ke-backend.md` mục 1.3.

---

## 1. Quy ước chung

### 1.1 Format lỗi (mọi endpoint)

```json
{ "timestamp": "2026-01-01T10:00:00Z", "status": 400,
  "code": "TASK_DEADLINE_INVALID", "message": "Deadline không hợp lệ",
  "path": "/api/v1/tasks" }
```

### 1.2 Phân trang

Dùng cho danh sách dài (tin nhắn, thông báo, audit log): `?page=0&size=20` → trả `content`, `totalElements`, `totalPages`.

### 1.3 Vai trò trong header/token

JWT chứa `userId`, `email`. Vai trò theo dự án lấy từ bảng `project_members` (không nhồi vào token — đổi vai trò có hiệu lực ngay).

---

## 2. Auth

| Method & Path | Auth | Request (chính) | Response (chính) |
|---|---|---|---|
| `POST /auth/register` | Public | `{email, password, fullName}` | `{id, email, fullName}` |
| `POST /auth/login` | Public | `{email, password}` | `{accessToken, refreshToken, expiresAt, user}` |
| `POST /auth/refresh` | Public | `{refreshToken}` | `{accessToken, refreshToken}` (cấp mới) |
| `POST /auth/logout` | Bearer | `{refreshToken}` | `204` (thu hồi refresh token) |
| `POST /auth/forgot-password` | Public | `{email}` | `204` (luôn 204, không tiết lộ email tồn tại) |
| `POST /auth/reset-password` | Public | `{token, newPassword}` | `204` |

## 3. User & Device

| Method & Path | Auth | Request (chính) | Response (chính) |
|---|---|---|---|
| `GET /users/me` | Bearer | — | `{id, email, fullName, avatarUrl}` |
| `PATCH /users/me` | Bearer | `{fullName?, avatarUrl?}` | user DTO |
| `POST /users/me/password` | Bearer | `{oldPassword, newPassword}` | `204` |
| `POST /users/me/avatar` | Bearer | multipart `file` | `{avatarUrl}` |
| `POST /devices/token` | Bearer | `{fcmToken, platform}` | `204` |
| `DELETE /devices/token` | Bearer | `{fcmToken}` | `204` |

## 4. Dự án, thành viên, link mời

| Method & Path | Quyền | Request (chính) | Response (chính) |
|---|---|---|---|
| `GET /projects` | Bearer | — | danh sách dự án mình tham gia (kèm vai trò) |
| `POST /projects` | Bearer | `{name, description?}` | project DTO (người tạo = OWNER) |
| `GET /projects/{id}` | Member+ | — | project DTO |
| `PATCH /projects/{id}` | Owner/Admin | `{name?, description?}` | project DTO |
| `DELETE /projects/{id}` | Owner | — | `204` (soft delete) |
| `GET /projects/{id}/members` | Member+ | — | `[{userId, fullName, email, role}]` |
| `PATCH /projects/{id}/members/{userId}` | Owner/Admin | `{role}` | member DTO |
| `DELETE /projects/{id}/members/{userId}` | Owner/Admin | — | `204` |
| `POST /projects/{id}/invites` | Owner/Admin | `{expiresAt?}` | `{code, inviteUrl}` |
| `GET /projects/{id}/invites` | Owner/Admin | — | danh sách link mời còn hiệu lực |
| `DELETE /projects/{id}/invites/{inviteId}` | Owner/Admin | — | `204` (thu hồi link) |
| `POST /projects/join` | Bearer | `{code}` | project DTO (vai trò MEMBER) |

## 5. Task, nhãn, checklist, bình luận, đính kèm

| Method & Path | Quyền | Request (chính) | Response (chính) |
|---|---|---|---|
| `GET /projects/{id}/tasks` | Member+ | — | toàn bộ task của dự án (client sort/filter) |
| `POST /tasks` | Member+ | `{projectId, title, description?, status?, priority?, startDate?, deadline?, labelIds?, assigneeIds?, parentTaskId?}` | task DTO |
| `GET /tasks/{id}` | Member+ | — | task DTO (kèm assignees, labels, subtasks, comments) |
| `PATCH /tasks/{id}` | Member+ | các trường cần sửa | task DTO |
| `DELETE /tasks/{id}` | Member+ | — | `204` (soft delete) |
| `PATCH /tasks/{id}/status` | Member+ | `{status}` | task DTO |
| `PATCH /tasks/{id}/position` | Member+ | `{status, position}` | `204` (Kanban kéo thả) |
| `PATCH /tasks/{id}/assignees` | Member+ | `{assigneeIds: []}` | task DTO (gán mới → push) |
| `GET /projects/{id}/labels` · `POST /projects/{id}/labels` · `DELETE /labels/{id}` | Member+ | `{name, color}` | label DTO |
| `POST /tasks/{id}/subtasks` | Member+ | `{title}` | task DTO (subtask) |
| `PATCH/DELETE /tasks/{id}/subtasks/{sid}` | Member+ | `{title?, done?}` | `204` |
| `GET /tasks/{id}/comments` | Member+ | — | danh sách comment |
| `POST /tasks/{id}/comments` | Member+ | `{content, mentionUserIds?}` | comment DTO (mention → push) |
| `PATCH/DELETE /tasks/{id}/comments/{cid}` | Tác giả | `{content}` | `204` |
| `POST /tasks/{id}/attachments` | Member+ | multipart `file` | file DTO |
| `DELETE /tasks/{id}/attachments/{fid}` | Member+/Uploader | — | `204` |

## 6. Lịch cá nhân

| Method & Path | Auth | Request (chính) | Response (chính) |
|---|---|---|---|
| `GET /schedule/events?from=&to=` | Bearer | — | sự kiện cá nhân trong khoảng |
| `POST /schedule/events` | Bearer | `{title, description?, startTime, endTime, taskId?}` | event DTO |
| `PATCH /schedule/events/{id}` | Chủ sự kiện | các trường cần sửa | event DTO |
| `DELETE /schedule/events/{id}` | Chủ sự kiện | — | `204` |

## 7. Thống kê ("Hôm nay", dashboard)

| Method & Path | Quyền | Response (chính) |
|---|---|---|
| `GET /me/today` | Bearer | `{overdueTasks, dueTodayTasks, upcomingTasks (7 ngày), todayEvents}` |
| `GET /me/stats?from=&to=` | Bearer | `{completedPerDay: [], completedPerWeek: [], completedPerMonth: [], onTimeRate}` |
| `GET /projects/{id}/stats` | Member+ | `{totalTasks, perStatus: {}, percentDone, overdueCount, perMember: [{userId, total, done}]}` |

## 8. Chat

| Method & Path | Quyền | Request (chính) | Response (chính) |
|---|---|---|---|
| `GET /conversations` | Bearer | — | hội thoại của mình (GROUP theo dự án + DIRECT), kèm `unreadCount`, tin cuối |
| `POST /conversations` | Bearer | `{type: DIRECT, userId}` | conversation DTO (idempotent theo cặp user) |
| `GET /conversations/{id}/messages?before=&size=` | Thành viên | — | lịch sử tin (phân trang ngược) |
| `POST /conversations/{id}/messages` | Thành viên | `{content, replyToId?, clientMessageId}` | message DTO (fallback khi WS lỗi) |
| `PATCH /conversations/{id}/read` | Thành viên | `{lastReadMessageId}` | `204` (broadcast read receipt qua WS) |

## 9. Thông báo & thiết bị

| Method & Path | Auth | Request (chính) | Response (chính) |
|---|---|---|---|
| `GET /notifications?page=&size=` | Bearer | — | danh sách thông báo |
| `GET /notifications/unread-count` | Bearer | — | `{count}` |
| `PATCH /notifications/{id}/read` | Bearer | — | `204` |
| `PATCH /notifications/read-all` | Bearer | — | `204` |

## 10. Audit log

| Method & Path | Quyền | Response (chính) |
|---|---|---|
| `GET /projects/{id}/audit-logs?page=&size=&action=` | Owner | nhật ký hoạt động của dự án |

## 11. File

| Method & Path | Quyền | Request (chính) | Response (chính) |
|---|---|---|---|
| `POST /files` | Bearer | multipart `file` (+ `taskId`/`messageId` nếu có) | file DTO `{id, name, size, mimeType, url}` |
| `GET /files/{id}` | Member+ của dự án liên quan | — | stream nội dung file |

---

## 12. WebSocket (STOMP)

| Hướng | Destination | Payload | Ghi chú |
|---|---|---|---|
| Handshake | `/ws` | JWT qua header `Authorization` hoặc query `?token=` | HandshakeInterceptor kiểm tra |
| Client → Server | `/app/chat.send` | `{conversationId, content, replyToId?, clientMessageId}` | tương đương REST nhưng realtime |
| Client → Server | `/app/conversations.read` | `{conversationId, lastReadMessageId}` | read receipt |
| Server → Client | `/topic/conversations.{id}` | `MessageDto` / `ReadReceiptDto` | thành viên đang mở hội thoại |
| Server → Client | `/user/queue/notifications` | `NotificationDto` | thông báo cá nhân khi app mở |

---

## 13. Mã lỗi chính

| Code | HTTP | Ý nghĩa |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | Sai email/mật khẩu |
| `AUTH_TOKEN_EXPIRED` | 401 | Access token hết hạn |
| `AUTH_REFRESH_INVALID` | 401 | Refresh token sai/đã thu hồi |
| `USER_EMAIL_EXISTS` | 409 | Email đã đăng ký |
| `RESET_TOKEN_INVALID` | 400 | Token đặt lại mật khẩu sai/hết hạn |
| `PROJECT_NOT_FOUND` | 404 | Không thấy dự án |
| `PROJECT_FORBIDDEN` | 403 | Không đủ vai trò |
| `INVITE_INVALID` | 400 | Link mời sai/thu hồi/hết hạn |
| `TASK_NOT_FOUND` | 404 | Không thấy task |
| `TASK_DEADLINE_INVALID` | 400 | Deadline/start_date không hợp lệ |
| `MEMBER_NOT_FOUND` | 404 | Không thấy thành viên dự án |
| `FILE_TOO_LARGE` / `FILE_TYPE_NOT_ALLOWED` | 400 | Quá 10MB / sai loại file |
| `CONVERSATION_FORBIDDEN` | 403 | Không phải thành viên hội thoại |
| `VALIDATION_ERROR` | 400 | Lỗi validate DTO |
| `INTERNAL_ERROR` | 500 | Lỗi không xác định |
