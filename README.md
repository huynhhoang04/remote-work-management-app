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
 ├─ android/            # (sau) App Kotlin — Gradle project
 └─ infra/              # (sau) docker-compose, nginx
```

---

## Chạy backend

Yêu cầu: **JDK 21**, **Maven 3.9+** (hoặc dùng `./mvnw`), **PostgreSQL 16**, **Docker** (cho integration test).

```bash
# 1. PostgreSQL
docker run -d --name remoteapp-db -p 5432:5432 \
  -e POSTGRES_DB=remoteapp -e POSTGRES_USER=remoteapp -e POSTGRES_PASSWORD=remoteapp \
  postgres:16

# 2. Chạy backend (profile mặc định đọc env DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET)
cd backend
./mvnw spring-boot:run

# Swagger UI:  http://localhost:8080/swagger-ui.html
# Health:      http://localhost:8080/actuator/health
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
| [`docs/baocao-25-09-2026/`](docs/baocao-25-09-2026/) | Tài liệu báo cáo BTL 25/09/2026 (mô tả, công nghệ, khảo sát) |

> `docs/04` + `docs/plan-backend/` là nguồn **hiện hành** cho backend; `docs/01–03` là bản gốc,
> một phần đã bị điều chỉnh (bảng đối chiếu trong `docs/04` mục 1.3).

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
