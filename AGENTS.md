# AGENTS.md

Hướng dẫn cho AI coding agent (và con người) khi làm việc trong repo này. Đọc file này trước,
rồi đọc tài liệu liên quan trong `docs/` trước khi viết code.

## 1. Dự án này là gì

**Đề tài 21 — Ứng dụng quản lý và theo dõi làm việc từ xa** (đồ án môn Mobile App):
app Android cho cá nhân/nhóm nhỏ: quản lý dự án + task (Kanban, timeline), nhắc deadline,
thống kê năng suất, chat theo dự án, thông báo push. Backend là **Spring Boot modular monolith**
hiện phục vụ app mobile (sau này có thể thêm client khác).

Phạm vi đã được **rà soát và cắt giảm** theo góp ý người hướng dẫn: giữ chức năng "nên có của
một ứng dụng di động", cắt chức năng "đậm chất desktop". Danh sách giữ/cắt cụ thể nằm ở
`docs/04-thiet-ke-backend.md` mục 1 — **không** thêm lại chức năng đã bị cắt.

## 2. Ràng buộc cứng (không vi phạm)

| Ràng buộc | Giá trị |
|---|---|
| Backend | **Java 21 LTS + Spring Boot 3.3.x** |
| Build | **Maven** (dùng `./mvnw`) |
| Kiến trúc | **Modular monolith** — 1 app, 1 PostgreSQL. KHÔNG microservices, không message broker, không API gateway |
| CSDL | **PostgreSQL 16**, schema do **Flyway** quản lý (`ddl-auto: validate`) |
| Xác thực | JWT (access 15' + refresh xoay vòng), RBAC 4 vai trò theo dự án |
| Mobile | Kotlin + Jetpack Compose (chưa khởi tạo trong repo) |

**Đã bị loại trừ có chủ đích:** Redis, MinIO (file lưu đĩa local), workspace/tổ chức, video call
WebRTC, chat typing/presence, đồng bộ offline 2 chiều, import/export, audit nâng cao theo tài liệu.
Đừng reintroduce chúng khi chưa có quyết định ghi trong `docs/`.

## 3. Bản đồ repo

```
backend/                     Spring Boot modular monolith (Maven)
├─ src/main/java/com/example/remoteapp/
│   ├─ config/               Security, WebSocket, OpenAPI
│   ├─ common/               BaseEntity, ErrorCode, AppException, GlobalExceptionHandler, SecurityUtils
│   ├─ auth/                 JWT, refresh token, đăng ký/đăng nhập/quên mật khẩu
│   ├─ user/                 hồ sơ, device FCM
│   ├─ project/              dự án, thành viên, RBAC (ProjectPermissionService), link mời
│   ├─ audit/                audit log (leaf module)
│   └─ (task, schedule, stats, chat, notification, file)  ← các module theo plan
├─ src/main/resources/db/migration/   Flyway V1, V2, ...
└─ src/test/java/            unit test (Mockito) + integration test (Testcontainers)
docs/                        tài liệu dự án (xem mục 4)
.github/workflows/           CI backend (./mvnw test)
.agents/skills/              kỹ năng hỗ trợ thiết kế mobile app
android/, infra/             (chưa có) app Kotlin, hạ tầng deploy
```

## 4. Tài liệu — đọc cái nào trước

| Ưu tiên | Tài liệu | Dùng cho |
|---|---|---|
| 1 | `docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md` | Đặc tả + implementation từng chức năng (mã FR, luật nghiệp vụ) |
| 2 | `docs/plan-backend/02-danh-sach-api.md` | Hợp đồng API + mã lỗi |
| 3 | `docs/plan-backend/01-kien-truc-va-stack.md` | Kiến trúc, stack, dependency rules, quy ước |
| 4 | `docs/04-thiet-ke-backend.md` | Phạm vi giữ/cắt, ERD, RBAC, lộ trình |
| — | `docs/01–03`, `docs/baocao-*` | Bối cảnh/bản gốc — đã bị `docs/04` điều chỉnh một phần |

Đổi API/entity/bảng → cập nhật tài liệu tương ứng **trong cùng commit**.

## 5. Quy ước backend

- **Phân tầng:** `controller → service → repository → entity`. Controller không viết logic
  nghiệp vụ; **không** trả entity ra ngoài — luôn dùng DTO (`record`).
- **Dependency rules (module):** không truy cập repository của module khác (ngoại lệ `stats`
  query đọc tổng hợp). Gọi module khác qua public service. Chi tiết: `docs/plan-backend/01` §4.2.
- **Validation:** DTO dùng Jakarta (`@NotBlank`, `@Size`, `@Email`...); service kiểm tra tiếp
  luật nghiệp vụ (duy nhất, quyền, trạng thái...).
- **Lỗi:** ném `AppException(ErrorCode, message)`; `GlobalExceptionHandler` trả format chuẩn
  (`timestamp/status/code/message/path`). Không ném exception kiểu khác từ service.
- **Phân quyền:** mọi thao tác theo dự án phải qua
  `ProjectPermissionService.requireRole/requireMember` (thực thi ở backend, không tin UI).
- **Người dùng hiện tại:** controller lấy qua `SecurityUtils.currentUserId()` rồi **truyền
  tường minh** `actorId` vào service (service không đọc SecurityContext → dễ test).
- **Audit:** hành động quan trọng gọi `AuditService.record(...)` trong transaction.
- **Entity:** kế thừa `BaseEntity` (UUID id, createdAt, updatedAt), `@Enumerated(EnumType.STRING)`,
  soft delete bằng `deletedAt`; Lombok `@Getter/@Setter`; mapping entity ↔ DTO thủ công (không MapStruct).
- **Javadoc:** mọi method public có Javadoc + dòng `@reference` trỏ tới mục tương ứng trong
  `docs/plan-backend/03` (hoặc `docs/04`) — codebase hiện tại tuân thủ điều này.
- **Ngôn ngữ:** identifier tiếng Anh; comment/Javadoc/message lỗi tiếng Việt.
- **Config:** biến môi trường (`DB_URL`, `JWT_SECRET`, `MAIL_*`...), không commit secret.

## 6. Database & Migration

- Bảng mới = file Flyway mới (`V3__*.sql`, `V4__*.sql`...). **Không** sửa file migration đã commit.
- Khớp chính xác entity ↔ schema (đang `ddl-auto: validate`): kiểu cột, tên cột, NOT NULL.
- Index cho các cột query nhiều (`project_id`, `deadline`, `user_id`...). Khoá ngoại tham chiếu
  `users(id)`/`projects(id)` trong cùng DB.

## 7. Test (bắt buộc)

- Unit test service bằng **Mockito** cho mọi luật nghiệp vụ (phân quyền, validate, idempotent...).
- Integration test `@SpringBootTest` + MockMvc + **Testcontainers PostgreSQL**:
  `@Testcontainers(disabledWithoutDocker = true)` — tự skip khi không có Docker, CI vẫn chạy.
- Trước khi xong việc: `cd backend && ./mvnw test` phải **xanh**. Thay đổi API → cập nhật/Thêm test.

## 8. Lệnh chính

```bash
cd backend
./mvnw clean verify        # build + toàn bộ test
./mvnw test                # chỉ test
./mvnw spring-boot:run     # chạy local (cần PostgreSQL)
```

CI: `.github/workflows/backend-ci.yml` chạy `./mvnw -B test` trên JDK 21 mỗi push/PR.

## 9. Cách làm việc ở đây

1. **Đọc trước khi viết.** Đọc tài liệu mục 4 + file sắp sửa. Không sửa file chưa đọc.
2. **Bám plan.** Thứ tự triển khai: `docs/plan-backend/03` mục I (bước 1–2 ✅, tiếp theo là
   module task). Yêu cầu trái plan/giải pháp bất thường → hỏi hoặc ghi rõ lý do.
3. **Giữ scope chặt.** Chỉ làm phần được yêu cầu — không abstraction thừa, không refactor ngoài
   phạm vi, không thêm cấu hình/hạ tầng mới khi chưa cần.
4. **Validate trước khi báo xanh.** Build + test phải pass; sửa thứ mình làm hỏng.
5. **Giữ `main` xanh.** Không commit build/test đỏ.
6. **Commit:** Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`...); không commit
   `target/`, `uploads/`, secret.

## 10. Anti-patterns

- Bỏ qua `ProjectPermissionService` rồi query thẳng `project_members`.
- Chỉ validate ở UI/DTO mà không kiểm tra luật ở service.
- Trả entity từ controller, hoặc dùng `ddl-auto: update` thay vì Flyway.
- Sửa/đổi file migration cũ đã commit.
- Tách service thành microservice, thêm Redis/MinIO/Kafka "cho chuyên nghiệp".
- Thêm lại chức năng đã bị cắt ở `docs/04` §1.2 khi chưa có quyết định mới.
- Method public không có Javadoc/`@reference` (lệch chuẩn dự án).
- Commit `.env`, `JWT_SECRET` thật, hoặc dữ liệu người dùng thật.
