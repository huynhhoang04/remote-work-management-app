# Ứng dụng Quản lý & Theo dõi Làm việc Từ xa

> **Đồ án Mobile App — Đề tài 21**
> React Native (Expo + TypeScript) · Backend Java 21 + Spring Boot · PostgreSQL

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
| Kế hoạch CI/CD phase 1 (Render + Neon) | ✅ Đã triển khai — `docs/plan-deploy/01-ci-cd-phase-1.md` |
| Kế hoạch app mobile phase 1 (React Native/Expo) | 📝 `docs/plan-app/01-phase-1.md` |
| Backend phase 1: auth, user/device, dự án + RBAC + link mời, audit log | ✅ Test xanh, có CI |
| Backend phase 2: task, lịch cá nhân, thống kê, thông báo/FCM, chat WS, file | ✅ **74 test + 103 request Bruno xanh** — `docs/plan-backend/04-tong-ket-trien-khai-backend.md` |
| Dịch vụ ngoài (Supabase Storage, Firebase FCM) | 🛠 Hướng dẫn setup — `docs/plan-deploy/02-dich-vu-ngoai.md` |
| Ứng dụng mobile (React Native/Expo) — phase 1 | 🚧 Theo plan `docs/plan-app/01-phase-1.md` |

**Phạm vi rút gọn theo góp ý người hướng dẫn:** tập trung chức năng "nên có của một ứng dụng
di động", cắt các chức năng "đậm chất desktop" (video call, import/export, timeline chi tiết,
workspace tổ chức...). Xem chi tiết: `docs/04-thiet-ke-backend.md` mục 1.

---

## Cấu trúc thư mục

```
MobileAppProject/
 ├─ backend/            # Spring Boot — modular monolith (Maven)
 ├─ bruno/              # Bruno workspace — 103 request, phủ 65 route (OpenCollection YAML)
 ├─ docs/               # Tài liệu dự án (xem mục lục bên dưới)
 ├─ .github/workflows/  # CI: build + test backend
 ├─ .agents/skills/     # Kỹ năng hỗ trợ thiết kế mobile app
 ├─ infra/              # docker-compose dev (PostgreSQL, mailpit)
 ├─ mobile/             # App React Native (Expo + TypeScript) — Android APK
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
./mvnw test             # chỉ chạy test (74 test, gồm 2 integration Testcontainers)
```

- Integration test dùng **Testcontainers** (PostgreSQL thật) — tự bỏ qua nếu máy không bật Docker
  daemon, CI trên GitHub Actions vẫn chạy đủ.
- Đổi mật khẩu cấu hình qua biến môi trường: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
  `MAIL_HOST`, `MAIL_PORT`, `STORAGE_*`, `FCM_*`... — không commit secret.
- Test API đầy đủ bằng Bruno (65 route):

  ```bash
  cd bruno
  ./run.sh --bail          # full flow trên backend local :8080
  ./run.sh --tags tasks    # chỉ một nhóm
  ```

  Coverage matrix route → request: `bruno/README.md`.

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
| [`docs/plan-backend/04-tong-ket-trien-khai-backend.md`](docs/plan-backend/04-tong-ket-trien-khai-backend.md) | **Bàn giao backend**: chức năng, bản đồ file, 65 endpoint, test, env |
| [`docs/plan-backend/05-schema-co-so-du-lieu.md`](docs/plan-backend/05-schema-co-so-du-lieu.md) | **Schema DB**: ERD 19 bảng + catalog cột/index/FK + query mẫu |
| [`docs/plan-deploy/01-ci-cd-phase-1.md`](docs/plan-deploy/01-ci-cd-phase-1.md) | Plan CI/CD phase 1: Render (Docker) + Neon (PostgreSQL), deploy hook từ GitHub Actions |
| [`docs/plan-deploy/02-dich-vu-ngoai.md`](docs/plan-deploy/02-dich-vu-ngoai.md) | Setup Supabase Storage + Firebase FCM (và phương án Cloudinary/Firebase Storage/MinIO) |
| [`docs/plan-deploy/03-deploy-phase-2.md`](docs/plan-deploy/03-deploy-phase-2.md) | **Runbook deploy phase 2**: env mới, thứ tự Supabase → push → verify → rollback |
| [`docs/plan-app/01-phase-1.md`](docs/plan-app/01-phase-1.md) | Plan app mobile phase 1 — React Native (Expo + TypeScript), design system & khai báo màn hình chi tiết |
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
  Object storage: `STORAGE_TYPE=supabase`, `STORAGE_BASE_URL`, `STORAGE_SERVICE_KEY` (secret),
  `STORAGE_BUCKET`, `STORAGE_PUBLIC_URL`. Push: `FCM_ENABLED`, `FCM_CREDENTIALS_BASE64` (secret).
  Hướng dẫn: `docs/plan-deploy/02-dich-vu-ngoai.md`.
- **Free tier:** service ngủ sau ~15 phút (request đầu 30–60 s), Neon autosuspend; Render free
  **không có persistent disk** → dùng Supabase Storage (hoặc đĩa local chỉ khi dev).
- **Smoke test sau deploy:**

  ```bash
  cd bruno/collections/00-smoke
  npx -y @usebruno/cli run --env-file ../../environments/render.yml
  ```

- **Rollback:** Render → Deploys → chọn bản cũ → **Redeploy** (hoặc revert commit).
- Chi tiết từng bước: `docs/plan-deploy/01-ci-cd-phase-1.md`.

## Chạy app mobile (Expo + React Native)

```bash
cd mobile
npm ci
cp .env.example .env          # EXPO_PUBLIC_API_URL (mặc định backend Render)
npx expo start                # quét QR bằng Expo Go hoặc mở emulator
npx tsc --noEmit && npm test  # typecheck + unit/component tests
```

- Build APK: `npx expo run:android` (cần Android Studio/SDK) hoặc EAS Build
  (`npx eas build -p android --profile preview`, cần tài khoản expo.dev).
- Render free ngủ sau ~15 phút: lần mở app đầu có thể chờ 30–60 s — app hiện banner
  "Máy chủ đang khởi động" + nút thử lại.
- Cấu trúc: routes trong `mobile/src/app` (Expo Router), code dùng chung trong `mobile/src`
  (`api/`, `components/`, `features/`, `schemas/`, `store/`, `theme/`, `utils/`).

---

## Tech stack

**Backend (đã triển khai):** Java 21 · Spring Boot 3.3 · **Maven** · Spring Security + JWT (jjwt) ·
Spring Data JPA/Hibernate · Flyway · PostgreSQL 16 · Spring Mail · Spring WebSocket (STOMP) ·
firebase-admin (FCM) · Supabase Storage (REST, fallback đĩa local) · springdoc-openapi ·
JUnit5 + Mockito + Testcontainers.

**Mobile (phase 1):** React Native **Expo (managed)** · **TypeScript** · Expo Router · React Native Paper (M3) ·
TanStack Query · axios · zod + react-hook-form · expo-secure-store · Reanimated · Jest + RNTL.
Phase sau: nối API backend, FCM token, cache offline, chat realtime.

> **Ghi chú quyết định:** nhóm chuyển UI từ Kotlin/Java native sang **React Native** (vẫn build ra APK
> Android) vì quen React, cần UI/animation phong phú (Render free khởi động chậm) và không dùng Kotlin.
> Lý do chi tiết + kế hoạch: `docs/plan-app/01-phase-1.md`.

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
