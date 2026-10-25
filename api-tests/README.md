# Bruno — Remote Work Management API

Bruno workspace for testing the backend API (Đề tài 21). Built with the
`bruno-api-testing` skill (`.agents/skills/bruno-api-testing/SKILL.md`).

## Accounts and roles (đọc trước)

- **Tài khoản không có vai trò.** Hệ thống chỉ có 4 vai trò **theo từng dự án**:
  `OWNER > ADMIN > MEMBER > VIEWER`. Một tài khoản có thể là `OWNER` ở dự án này và `VIEWER`
  ở dự án khác — không cần đăng ký tài khoản mới khi vai trò thay đổi.
- Vì vậy workspace dùng **hai tài khoản trung tính A và B** (không phải "owner" vs "member"):
  A tạo dự án 1 (A = OWNER, B join → MEMBER rồi bị hạ xuống VIEWER), còn B tạo dự án 2 (B = OWNER).
  Bộ request 12 → 19 → 26 chứng minh điều này.
- Vai trò đọc từ DB mỗi request (không nhồi vào JWT) nên đổi vai trò có hiệu lực ngay.

## Import and pick an environment

1. In Bruno choose **Open Workspace** and open this folder (`api-tests/`).
2. Open the environment menu (top right) and pick **local**
   (`environments/local.yml`). This is required for `{{baseUrl}}` — without it every request
   fails with an unresolved variable.
3. **Mọi giá trị đầu vào là của bạn** — tên biến có tiền tố `input` để phân biệt rõ:
   `inputEmailA`, `inputEmailB`, `inputShortPwEmail`, `inputFcmToken` sửa ở tab **Vars**;
   `inputPasswordA`, `inputPasswordB`, `inputNewPasswordA` nhập ở tab **Secret Variables**.
   **Không có script nào ghi vào các biến này** (nên chúng luôn sửa được). Script chỉ **bắt kết quả**
   trả về (`tokenA`, `tokenB`, `userAId`, `projectId`…) và **assert**.
4. Nếu bạn từng chạy bản cũ (bản có script tự sinh email/password), các giá trị đó vẫn là
   **Runtime variable** trong phiên Bruno hiện tại và sẽ đè lên biến environment. Cách xoá: mở
   environment → tab **Vars** → mục Runtime variables → xoá từng biến (hoặc khởi động lại Bruno).
   Các biến đầu vào đã đổi sang tên `input*` nên không bị ảnh hưởng nữa.
5. Chạy lại cả collection: đổi `inputEmailA`/`inputEmailB` (hoặc dùng DB sạch) — nếu không, request
   01/02 sẽ trả 409 vì tài khoản đã tồn tại.

## Forgot / reset password flow (local)

`POST /auth/forgot-password` (request 08) **luôn trả 204** kể cả email không tồn tại — đó là thiết kế
(không tiết lộ email đã đăng ký). Email được gửi qua SMTP `localhost:1025`; nếu máy chưa có SMTP server
thì backend chỉ log warning và vẫn trả 204, nên bạn sẽ **không thấy gì**.

Để xem email thật:

```bash
# Cách 1: binary mailpit có sẵn trên máy (không cần Docker)
mailpit                       # SMTP :1025, UI/API :8025

# Cách 2: Docker
docker compose -f ../infra/docker-compose.yml up -d mailpit
```

Sau đó: chạy request 08 → mở <http://localhost:8025> → mở email → copy phần sau `app://reset?token=`
→ dán vào biến `inputResetToken` (tab Vars) + đặt `inputResetPassword` (tab Secret Variables)
→ chạy request reset ở collection **`02 - Auth reset (manual)`** (không nằm trong smoke suite vì cần
dán token tay). Token dùng một lần, hết hạn sau 30 phút.

Chạy collection reset bằng CLI:

```bash
cd collections/02-auth-reset
npx -y @usebruno/cli run --env-file ../../environments/local.yml \
  --env-var inputResetToken='<token-copy-tu-email>' \
  --env-var inputResetPassword='password-123-v3'
```

## Smoke test trên backend đã deploy (Render)

```bash
cd collections/00-smoke
npx -y @usebruno/cli run --env-file ../../environments/render.yml
```

Collection **`00 - Smoke (deployed)`**: 2 request read-only (`/actuator/health` → UP,
`/api/v1/users/me` → 401). Sửa `baseUrl` trong `environments/render.yml` theo URL Render thật.
Free tier ngủ sau ~15 phút nên lần chạy đầu có thể mất 30–60 s (timeout đã đặt 60 s).

## Running with the CLI (smoke test)

```bash
# backend đang chạy ở localhost:8080 (docker compose + ./mvnw spring-boot:run)
./run.sh                 # tự sinh email cho lần chạy + mật khẩu dev, truyền qua --env-var
./run.sh --bail          # dừng ngay khi fail
```

The CLI must be invoked **from inside a collection folder** and reads the environment with
`--env-file ../../environments/local.yml` (it does not auto-discover workspace-level YAML
environments) — `run.sh` does this for you.

## Request map

| # | Request | Covers |
|---|---|---|
| 01–04 | register/login user A, user B | FR-A01, A02 |
| 05–06 | `GET /users/me`, 401 without token | FR-A04, G04 |
| 07 | refresh token rotation | G04 |
| 08 | forgot password → 204 + email | A03 |
| 09–10 | FCM device token register/remove | A05 |
| 11 | change password + revoke sessions | A04 |
| 12–14 | project create/list/detail (A = OWNER) | B02 |
| 15–17 | invite link, join by code (B = MEMBER), member list | B03 |
| 18–19 | role → VIEWER, then 403 on project update | RBAC, G01 |
| 20–21 | audit log (owner) / 403 for VIEWER | G05 |
| 22 | invalid invite code → 400 | validate |
| 23 | **duplicate email → 409 `USER_EMAIL_EXISTS`** | validate (service + unique constraint) |
| 24 | **password < 8 ký tự → 400 `VALIDATION_ERROR`** | validate (DTO `@Size(min = 8)`) |
| 25 | wrong password → 401 `AUTH_INVALID_CREDENTIALS` | A02 |
| 26 | **B creates own project → B = OWNER** (roles per-project) | RBAC, G01 |

Requests 23–26 dùng số tiếp theo (không đánh lại số cũ) theo quy ước numbering của skill.

## Safety notes

- **Writes** (labelled in each request's `docs:`): `01`, `02`, `07` (rotate), `08`, `09`, `10`,
  `11`, `12`, `15`, `16`, `18`, `26`. In the Bruno app send them **one at a time** — never
  Collection Runner — and only against a disposable dev database.
  Requests `23`, `24`, `25` are negative tests rejected **before** any DB write.
- `run.sh` deliberately runs the whole collection against **local loopback only** (smoke test on a
  throwaway DB). Do not point it at staging/production.
- `11` revokes every refresh token of account A: run it after the refresh test; `{{refreshTokenA}}`
  stops working afterwards. `18` changes B's role in A's project, so `19`/`21` expect 403.
- Run order matters (tokens, `projectId`, `inviteCode` are runtime variables). Pin
  `projectId`/`inviteCode` if you need to run a single request out of order.
- Secrets never live in committed files. If a value was ever committed, rotate it.

## Layout notes (deviation from the skill template)

- One REST collection (`01-api`) instead of one per domain: the Bruno CLI can only run from a
  single collection root and runtime variables do not survive between CLI runs, so the whole flow
  lives in one collection. Requests are numbered in execution order and tagged (`auth`, `users`,
  `devices`, `projects`, `invites`, `audit`, `rbac`, `negative`).
- Collection `02-auth-reset` is **manual only** (needs the token pasted from the email), so the
  automated suite stays green without it.
- Format: **OpenCollection YAML** for the REST collection (per the skill); no gRPC collection —
  the backend exposes no gRPC.

## Inventory

- `00 - Smoke (deployed)`: 2 request read-only — health + 401 (chạy trên backend Render).
- `01 - Remote Work API (REST)`: 26 requests — auth 9 (01–04, 07, 08, 23, 24, 25), users 2 (05, 11),
  devices 2 (09, 10), projects 9 (12–14, 16–19, 22, 26), invites 1 (15), audit 2 (20, 21),
  unauthorized 1 (06) — trong đó 7 request là negative test (06, 19, 21, 22, 23, 24, 25).
- `02 - Auth reset (manual)`: 1 request — reset password bằng token lấy từ email (chạy tay).
