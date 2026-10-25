# Ứng dụng Quản lý & Theo dõi Làm việc Từ xa

> **Đồ án Mobile App — Đề tài 21**
> Android native (Kotlin) · Backend Java 21 + Spring Boot · PostgreSQL

Ứng dụng mobile giúp cá nhân và nhóm nhỏ làm việc từ xa lập kế hoạch, theo dõi tiến độ, cộng tác
trực tuyến và không bỏ sót deadline — **tất cả trong một app**, hoạt động offline ở mức xem,
miễn phí, tiếng Việt, tự host bằng Docker.

---

## Trạng thái dự án

| Hạng mục | Trạng thái |
|---|---|
| Tài liệu phân tích, kiến trúc, roadmap | ✅ `docs/01` → `docs/03` |
| Thiết kế backend v0.2 (đã rà soát & cắt giảm chức năng) | ✅ `docs/04` |
| Kế hoạch triển khai backend (kiến trúc, API, đặc tả) | ✅ `docs/plan-backend/` |
| Kế hoạch CI/CD phase 1 (Render + Neon) | 🛠 Đang setup — `docs/plan-deploy/01-ci-cd-phase-1.md` |
| Backend bước 1–2: auth, user/device, dự án + RBAC + link mời, audit log | ✅ Test xanh, có CI |
| Backend bước 3+: task, thống kê, nhắc nhở, chat | ⏭ Tiếp theo |
| Ứng dụng Android (Kotlin/Compose) | ⏳ Khởi tạo sau |

**Phạm vi rút gọn theo góp ý người hướng dẫn:** tập trung chức năng "nên có của một ứng dụng
di động", cắt các chức năng "đậm chất desktop" (video call, import/export, timeline chi tiết,
workspace tổ chức...). Xem chi tiết: `docs/04-thiet-ke-backend.md` mục 1.

---

## Cấu trúc thư mục

```
MobileAppProject/
 ├─ backend/            # Spring Boot — modular monolith (Maven)
 ├─ docs/               # Tài liệu dự án (xem mục lục bên dưới)
 ├─ .github/workflows/  # CI: build + test backend
 ├─ .agents/skills/     # Kỹ năng hỗ trợ thiết kế mobile app
 ├─ infra/              # docker-compose dev (PostgreSQL, mailpit)
 ├─ android/            # (sau) App Kotlin — Gradle project
 └─ .vscode/            # Cấu hình VSCode (JDK 21, extensions)
```

---

## Chạy backend

Yêu cầu: **JDK 21**, **Maven 3.9+** (hoặc dùng `./mvnw`), **PostgreSQL 16**, **Docker** (cho integration test).

```bash
# 1. Hạ tầng local: PostgreSQL 16 + mailpit (cần Docker đang chạy)
docker compose -f infra/docker-compose.yml up -d

# 2. Chạy backend (mặc định đọc env DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET)
cd backend
./mvnw spring-boot:run

# Swagger UI:  http://localhost:8080/swagger-ui.html
# Health:      http://localhost:8080/actuator/health
# Mailpit:     http://localhost:8025 (xem email quên mật khẩu khi dev)
```

## Build & Test

```bash
cd backend
./mvnw clean verify     # build + toàn bộ test (unit + integration)
./mvnw test             # chỉ chạy test
```

- Integration test dùng **Testcontainers** (PostgreSQL thật) — tự bỏ qua nếu máy không bật Docker
  daemon, CI trên GitHub Actions vẫn chạy đủ.
- Đổi mật khẩu cấu hình qua biến môi trường: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
  `MAIL_HOST`, `MAIL_PORT`... — không commit secret.

## Gửi email: mailpit (dev) vs SMTP thật

- **Dev (mặc định):** backend gửi SMTP tới `localhost:1025` = **mailpit** — mail bị chặn ở đó, **không
  tới hộp thư thật**. Chạy `mailpit` (UI http://localhost:8025) hoặc `docker compose -f infra/docker-compose.yml up -d mailpit`.
- **SMTP thật** (khi app chạy với backend thật): chỉ cần cấu hình backend bằng biến môi trường,
  **không sửa code**. Ví dụ Gmail (cần bật 2FA + App Password):

```bash
MAIL_HOST=smtp.gmail.com MAIL_PORT=587 \
MAIL_USERNAME=you@gmail.com MAIL_PASSWORD='<app-password>' \
MAIL_SMTP_AUTH=true MAIL_SMTP_STARTTLS=true MAIL_FROM=you@gmail.com \
./mvnw spring-boot:run
```

  Có thể dùng Brevo/SendGrid/Amazon SES... cùng cơ chế. Ứng dụng Android không tự gửi mail — nó gọi
  backend, nên chỉ backend cần SMTP đúng. Nếu SMTP lỗi, API vẫn trả 204 (không lộ email tồn tại) và
  chỉ ghi warning trong log backend.
- Sender phải là địa chỉ đã xác thực (SPF/DKIM) để vào inbox; Gmail giới hạn ~500 mail/ngày.

---

## Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [`docs/01-phan-tich-yeu-cau.md`](docs/01-phan-tich-yeu-cau.md) | SRS: tác nhân, 6 nhóm chức năng, use case, user story, NFR |
| [`docs/02-kien-truc-va-tech-stack.md`](docs/02-kien-truc-va-tech-stack.md) | Kiến trúc tổng thể, tech stack, realtime, offline, CSDL (bản gốc) |
| [`docs/03-roadmap-va-mvp.md`](docs/03-roadmap-va-mvp.md) | MVP, 9 giai đoạn triển khai, backlog, test plan, rủi ro |
| [`docs/04-thiet-ke-backend.md`](docs/04-thiet-ke-backend.md) | **Thiết kế backend v0.2** — phạm vi giữ/cắt, kiến trúc, ERD, API, RBAC |
| [`docs/plan-backend/01-kien-truc-va-stack.md`](docs/plan-backend/01-kien-truc-va-stack.md) | Kiến trúc dự án + stack (Maven), cấu trúc package, dependency rules |
| [`docs/plan-backend/02-danh-sach-api.md`](docs/plan-backend/02-danh-sach-api.md) | Danh sách API REST + WebSocket, mã lỗi |
| [`docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md`](docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md) | Đặc tả + cách implementation từng chức năng |
| [`docs/plan-deploy/01-ci-cd-phase-1.md`](docs/plan-deploy/01-ci-cd-phase-1.md) | Plan CI/CD phase 1: Render (Docker) + Neon (PostgreSQL), deploy hook từ GitHub Actions |
| [`docs/baocao-25-09-2026/`](docs/baocao-25-09-2026/) | Tài liệu báo cáo BTL 25/09/2026 (mô tả, công nghệ, khảo sát) |

> `docs/04` + `docs/plan-backend/` là nguồn **hiện hành** cho backend; `docs/01–03` là bản gốc,
> một phần đã bị điều chỉnh (bảng đối chiếu trong `docs/04` mục 1.3).

## Deploy (phase 1 — Render + Neon)

- **Backend:** Render free — runtime **Docker**, region Singapore: `https://<service>.onrender.com`
  (điền URL thật sau khi tạo service). Config trong `render.yaml`.
- **Database:** Neon free (PostgreSQL 16, `sslmode=require`); Flyway tự chạy migration khi khởi động.
- **CI/CD:** push `main` → GitHub Actions chạy test + `docker build` → gọi **Render Deploy Hook**
  (GitHub secret `RENDER_DEPLOY_HOOK_URL`). `autoDeploy: false` nên test đỏ **không** deploy.
  Khi chưa tạo secret, job `deploy` bỏ qua kèm notice (không làm đỏ CI).
- **Env vars trên Render:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (secret),
  `DB_POOL_SIZE=5`, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70 -Duser.timezone=UTC`.
  Tuỳ chọn SMTP thật: `MAIL_*`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_FROM`.
- **Free tier:** service ngủ sau ~15 phút (request đầu 30–60 s), Neon autosuspend; Render free
  **không có persistent disk** → file upload (phase task/file) sẽ dùng object storage.
- **Smoke test sau deploy:**

  ```bash
  cd api-tests/collections/00-smoke
  npx -y @usebruno/cli run --env-file ../../environments/render.yml
  ```

- **Rollback:** Render → Deploys → chọn bản cũ → **Redeploy** (hoặc revert commit).
- Chi tiết từng bước: `docs/plan-deploy/01-ci-cd-phase-1.md`.

---

## Tech stack

**Backend (đã triển khai):** Java 21 · Spring Boot 3.3 · **Maven** · Spring Security + JWT (jjwt) ·
Spring Data JPA/Hibernate · Flyway · PostgreSQL 16 · Spring Mail · springdoc-openapi · JUnit5 + Mockito + Testcontainers.

**Mobile (dự kiến):** Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt ·
Room (cache offline) · Retrofit · WebSocket (STOMP) · FCM · WorkManager.

**Kiến trúc backend:** modular monolith — một ứng dụng, một CSDL, chia module theo feature
(auth, user, project, task, ...). **Không** microservices, không message broker.

---

## Hướng dẫn cho AI coding agent

Xem [`AGENTS.md`](AGENTS.md) — quy ước code, dependency rules giữa các module, quy trình làm việc
và các anti-pattern cần tránh.

---

## Quy trình làm việc

- Commit theo **Conventional Commits** (`feat:`, `fix:`, `docs:`...).
- Push → GitHub Actions chạy build + test backend (`.github/workflows/backend-ci.yml`).
- Đổi API/entity → cập nhật tài liệu tương ứng trong cùng một commit.
