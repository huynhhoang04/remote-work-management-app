# Ứng dụng Quản lý & Theo dõi Làm việc Từ xa

> **Đồ án cuối kỳ môn Mobile App — Đề tài 21**
> Android native (Kotlin) · Backend Java Spring Boot · PostgreSQL

Ứng dụng giúp cá nhân và nhóm làm việc từ xa lập kế hoạch, theo dõi tiến độ, cộng tác trực tuyến và không bỏ sót deadline.

---

## Mục lục tài liệu

| Tài liệu | Nội dung |
|---|---|
| [`docs/01-phan-tich-yeu-cau.md`](docs/01-phan-tich-yeu-cau.md) | SRS: tác nhân, 6 nhóm chức năng, use case, user story, yêu cầu phi chức năng |
| [`docs/02-kien-truc-va-tech-stack.md`](docs/02-kien-truc-va-tech-stack.md) | Kiến trúc hệ thống, tech stack, thiết kế API, realtime, offline sync, CSDL, RBAC, hạ tầng |
| [`docs/03-roadmap-va-mvp.md`](docs/03-roadmap-va-mvp.md) | Định nghĩa MVP, 9 giai đoạn triển khai, backlog ưu tiên, test plan, rủi ro |

---

## Chức năng chính (theo đề tài)

1. **Lập kế hoạch & quản lý dự án** — lịch làm việc cá nhân, dự án, task, ưu tiên, gán thành viên.
2. **Theo dõi tiến độ & hiệu suất** — dashboard tiến độ, thống kê năng suất, ghi nhận thời gian.
3. **Hợp tác trực tuyến** — workspace, chat nhóm, họp video, chia sẻ tài liệu.
4. **Quản lý nhiệm vụ & nhắc nhở** — nhắc deadline tự động, push notification.
5. **Đồng bộ & tích hợp** — đồng bộ đa thiết bị, offline-first, tích hợp email/calendar.
6. **Bảo mật & phân quyền** — RBAC, JWT, mã hóa, audit log.

---

## Tech Stack tóm tắt

- **Mobile:** Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt · Room · Retrofit · WebSocket · FCM · WorkManager
- **Backend:** Java 21 · Spring Boot 3 · Spring Security (JWT) · JPA/Hibernate · WebSocket (STOMP) · Flyway
- **Data:** PostgreSQL · Redis · MinIO
- **Hạ tầng:** Docker Compose · Nginx · GitHub Actions · coturn (WebRTC)

---

## Trạng thái dự án

🚧 **Giai đoạn nghiên cứu** — đã có bộ tài liệu phân tích, thiết kế kiến trúc và lộ trình.
Tiếp theo: chốt deadline & phân công → khởi tạo repo `android/`, `backend/`, `infra/` → bắt đầu Phase 1.

---

## Cấu trúc thư mục dự kiến

```
MobileAppProject/
 ├─ android/     # Ứng dụng Kotlin (Gradle project)
 ├─ backend/     # Spring Boot (Gradle project)
 ├─ docs/        # Tài liệu
 ├─ infra/       # docker-compose, nginx, coturn
 └─ README.md
```
