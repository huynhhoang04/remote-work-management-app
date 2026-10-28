# Bruno — Remote Work Management API

Bruno workspace for testing the backend API (Đề tài 21). Built with the
`bruno-api-testing` skill (`.agents/skills/bruno-api-testing/SKILL.md`).

Đọc `docs/plan-backend/04-tong-ket-trien-khai-backend.md` để biết toàn bộ chức năng/file,
và `docs/plan-deploy/02-dich-vu-ngoai.md` để setup Supabase Storage + Firebase FCM.

## Accounts and roles (đọc trước)

- **Tài khoản không có vai trò.** Hệ thống chỉ có 4 vai trò **theo từng dự án**:
  `OWNER > ADMIN > MEMBER > VIEWER`. Một tài khoản có thể là `OWNER` ở dự án này và `VIEWER`
  ở dự án khác — không cần đăng ký tài khoản mới khi vai trò thay đổi.
- Workspace dùng **ba tài khoản trung tính A, B, C**:
  - A tạo dự án 1 (A = OWNER).
  - B join → MEMBER, được nâng ADMIN rồi hạ VIEWER, bị xoá rồi join lại (MEMBER).
  - B tạo dự án 2 (B = OWNER) để chứng minh vai trò theo dự án.
  - C **không thuộc dự án nào** — dùng cho test 403/INVITE_INVALID.
- Vai trò đọc từ DB mỗi request (không nhồi vào JWT) nên đổi vai trò có hiệu lực ngay.

## Import and pick an environment

1. In Bruno choose **Open Workspace** and open this folder (`bruno/`).
2. Open the environment menu (top right) and pick **local** (`environments/local.yml`).
   Required for `{{baseUrl}}`.
3. **Mọi giá trị đầu vào là của bạn** — tên biến có tiền tố `input`:
   `inputEmailA/B/C`, `inputShortPwEmail`, `inputFcmToken`, `inputUploadFile`, `inputAvatarFile`
   ở tab **Vars**; `inputPasswordA/B/C`, `inputNewPasswordA`, `inputResetPassword` ở tab
   **Secret Variables**. Không script nào ghi vào các biến này.
4. Script chỉ **bắt kết quả** (`tokenA/B/C`, `projectId`, `taskId`, `attachmentId`…) và **assert**.
5. Chạy lại cả collection: đổi `inputEmailA/B/C` (hoặc dùng DB sạch) — nếu không request
   01/02/27 sẽ trả 409.

## Running the full flow

```bash
# Backend local (Postgres + mailpit)
docker compose -f infra/docker-compose.yml up -d
cd backend && ./mvnw spring-boot:run

# Bruno (103 request, chạy tuần tự)
cd bruno && ./run.sh --bail
# hoặc chỉ một nhóm:
./run.sh --tags tasks
```

`run.sh` tự sinh email riêng cho lần chạy, mật khẩu dev, và đường dẫn tuyệt đối cho
`fixtures/sample.txt` / `fixtures/sample.png`. Không sửa file environment.

Chạy smoke read-only trên backend đã deploy (Render):

```bash
cd bruno/collections/00-smoke
npx -y @usebruno/cli run --env-file ../../environments/render.yml
```

## Forgot / reset password flow (local)

`POST /auth/forgot-password` (request 08) **luôn trả 204** kể cả email không tồn tại.
Email đi qua SMTP `localhost:1025` (mailpit). Chạy `mailpit` hoặc
`docker compose -f infra/docker-compose.yml up -d mailpit`, mở <http://localhost:8025>,
copy phần sau `app://reset?token=` rồi chạy collection **`02 - Auth reset (manual)`**:

```bash
cd bruno/collections/02-auth-reset
npx -y @usebruno/cli run --env-file ../../environments/local.yml \
  --env-var inputResetToken='<token-copy-tu-email>' \
  --env-var inputResetPassword='password-123-v3'
```

## Coverage matrix — 65/65 endpoint (64 route + health)

`01-api` phủ **toàn bộ** route controller. Số trong bảng là `seq` của request.

| Method + Path | Request |
|---|---|
| `POST /auth/register` | 01, 02, 23, 24, 27 |
| `POST /auth/login` | 03, 04, 25, 28 |
| `POST /auth/refresh` | 07, 103 (sau logout → 401) |
| `POST /auth/logout` | 102 |
| `POST /auth/forgot-password` | 08 |
| `POST /auth/reset-password` | `02-auth-reset/01` (thủ công, cần token từ email) |
| `GET /users/me` | 05, 06 (401), `00-smoke/02` (401) |
| `PATCH /users/me` | 80 |
| `POST /users/me/password` | 11 |
| `POST /users/me/avatar` | 81 |
| `POST /devices/token` | 09 |
| `DELETE /devices/token` | 10 |
| `GET /projects` | 13 |
| `POST /projects` | 12, 26 |
| `GET /projects/{id}` | 14, 101 (đã xoá → 404) |
| `PATCH /projects/{id}` | 92 (200), 19 (VIEWER → 403) |
| `DELETE /projects/{id}` | 100 |
| `GET /projects/{id}/members` | 17, 93 |
| `PATCH /projects/{id}/members/{userId}` | 18, 29, 84 |
| `DELETE /projects/{id}/members/{userId}` | 94 |
| `POST /projects/{id}/invites` | 15, 88 |
| `GET /projects/{id}/invites` | 89 |
| `DELETE /projects/{id}/invites/{inviteId}` | 90 |
| `POST /projects/join` | 16, 22 (mã sai → 400), 91 (đã thu hồi → 400), 95 |
| `GET /projects/{id}/audit-logs` | 20, 96 (lọc action), 21 (VIEWER → 403) |
| `GET /projects/{id}/tasks` | 33 |
| `POST /tasks` | 32, 37 (deadline sai → 400), 85 (VIEWER → 403) |
| `GET /tasks/{id}` | 34, 98 (đã xoá → 404) |
| `PATCH /tasks/{id}` | 35, 36 (version cũ → 409) |
| `DELETE /tasks/{id}` | 97 |
| `PATCH /tasks/{id}/status` | 38 |
| `PATCH /tasks/{id}/position` | 39 |
| `PATCH /tasks/{id}/assignees` | 40 |
| `GET /projects/{id}/labels` | 31 |
| `POST /projects/{id}/labels` | 30 |
| `DELETE /labels/{id}` | 99 |
| `POST /tasks/{id}/subtasks` | 41, 43 (lồng cấp 2 → 400) |
| `PATCH /tasks/{id}/subtasks/{sid}` | 42 |
| `DELETE /tasks/{id}/subtasks/{sid}` | 83 |
| `GET /tasks/{id}/comments` | 45 |
| `POST /tasks/{id}/comments` | 44, 46, 86 (VIEWER → 403) |
| `PATCH /tasks/{id}/comments/{cid}` | 48 (tác giả), 47 (người khác → 403) |
| `DELETE /tasks/{id}/comments/{cid}` | 49 |
| `POST /tasks/{id}/attachments` | 50 |
| `DELETE /tasks/{id}/attachments/{fid}` | 52 |
| `GET /schedule/events` | 54 |
| `POST /schedule/events` | 53, 56 (thời gian sai → 400) |
| `PATCH /schedule/events/{id}` | 55 |
| `DELETE /schedule/events/{id}` | 82 |
| `GET /me/today` | 57 |
| `GET /me/stats` | 58 |
| `GET /projects/{id}/stats` | 59 |
| `GET /notifications` | 60, 63 |
| `GET /notifications/unread-count` | 61, 66 |
| `PATCH /notifications/{id}/read` | 62, 64 (người khác → 404) |
| `PATCH /notifications/read-all` | 65 |
| `GET /conversations` | 67 |
| `POST /conversations` | 70, 71 (idempotent) |
| `GET /conversations/{id}/messages` | 69 |
| `POST /conversations/{id}/messages` | 68, 72, 73 (idempotent), 75 (không phải thành viên → 403), 87 (VIEWER → 403) |
| `PATCH /conversations/{id}/read` | 74 |
| `POST /files` | 76 |
| `GET /files/{id}` | 51, 77, 79 (đã xoá → 404) |
| `DELETE /files/{id}` | 78 |
| `GET /actuator/health` | `00-smoke/01` |

## WebSocket smoke (thủ công)

Bruno CLI không chạy được STOMP. Kiểm tra tay bằng Postman/`wscat`:

1. Kết nối `ws://localhost:8080/ws` (hoặc `wss://...onrender.com/ws`) với header
   `Authorization: Bearer <accessToken>` (hoặc query `?token=<accessToken>`).
2. `SUBSCRIBE` `/topic/conversations.{conversationId}`.
3. `SEND` `/app/chat.send` payload
   `{"conversationId":"...","content":"hi","clientMessageId":"<uuid>"}`.
4. Phải nhận frame `MESSAGE` trên topic; token sai → handshake 401.
5. `SUBSCRIBE` `/user/queue/notifications` để nhận thông báo realtime.

## Inventory

- `00 - Smoke (deployed)`: 2 request read-only — health + 401.
- `01 - Remote Work API (REST)`: **103 request** — auth 10, users 4, devices 2, projects 12,
  members 4, invites 5, audit 3, tasks 9, labels 3, subtasks 4, comments 6, attachments 3,
  schedule 5, stats 3, notifications 7, chat 9, files 4, cleanup/auth 10 (một số request
  thuộc nhiều nhóm).
- `02 - Auth reset (manual)`: 1 request — reset password bằng token lấy từ email.

Tags: `smoke`, `auth`, `users`, `devices`, `projects`, `members`, `invites`, `audit`, `tasks`,
`labels`, `subtasks`, `comments`, `attachments`, `schedule`, `stats`, `notifications`, `chat`,
`files`, `rbac`, `negative`, `setup`, `manual`.

## Safety notes

- **Writes** được ghi `SIDE EFFECT:` trong `docs:` của từng request. Chạy từng request trong
  Bruno app, không dùng Collection Runner trên DB thật.
- Thứ tự là một phần của test: token/project/task id là runtime variable. Request `102`
  (logout B) và `103` (refresh sau logout) phải ở cuối.
- `run.sh` chỉ dành cho **local/demo DB**. Nó không tự xoá dữ liệu; chạy lại cần email mới
  hoặc DB sạch (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;` rồi chạy lại backend).
- Secret luôn nằm ở tab Secret Variables hoặc `*.local.yml` (đã gitignore). Không commit giá trị thật.
- `fixtures/sample.txt` (text/plain) và `fixtures/sample.png` (1×1 PNG) là file upload mẫu —
  không chứa dữ liệu thật.

## Layout notes

- Một collection REST `01-api` (runtime variables không sống qua lần chạy CLI khác collection),
  request đánh số theo thứ tự chạy, tag theo module.
- `02-auth-reset` tách riêng vì cần dán token từ email.
- Format: **OpenCollection YAML**; multipart dùng `body.type: multipart-form` + `data[]`
  (`type: file`, `value` là **mảng** đường dẫn).
