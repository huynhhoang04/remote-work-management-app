# Tài liệu Roadmap & MVP

> Đề tài 21 — Ứng dụng quản lý và theo dõi làm việc từ xa
> Cách chia giai đoạn theo **mục tiêu và độ ưu tiên**, sắp theo thứ tự thực hiện.
> Độ phức tạp dùng thang `S` (nhỏ) · `M` (vừa) · `L` (lớn) — không phải thời gian.

---

## 1. Định nghĩa MVP

**MVP = phiên bản nhỏ nhất chạy được end-to-end, demo được vòng đời cốt lõi của đề tài:**

> Người dùng đăng nhập → tạo dự án → mời thành viên → tạo & gán task kèm deadline → thành viên nhận thông báo, cập nhật trạng thái → trưởng dự án xem tiến độ → trò chuyện trong kênh dự án.

**Trong MVP (Must):**
- Xác thực tài khoản (đăng ký/đăng nhập/JWT/refresh).
- Quản lý dự án + thành viên.
- Task: CRUD, gán, trạng thái, deadline, ưu tiên, Kanban board.
- Lịch làm việc cá nhân cơ bản.
- Dashboard tiến độ dự án.
- Chat nhóm theo dự án (realtime).
- Thông báo: push FCM + nhắc deadline + trung tâm thông báo.
- Đồng bộ nền cơ bản (offline cho task).

**Ngoài MVP (Should/Could — làm sau):**
- Video call nhóm · Chia sẻ tài liệu · Ghi nhận thời gian & thống kê nâng cao · Tích hợp email/calendar · Audit log · 2FA · Import/Export.

**Tiêu chí "MVP đạt":** một người ngoài nhóm có thể tự đăng ký 2 tài khoản, tạo dự án, giao việc và thấy thông báo + cập nhật realtime giữa 2 máy.

---

## 2. Nguyên tắc chia giai đoạn

1. **Xương sống trước, tính năng sau:** auth + hạ tầng + dữ liệu nền phải xong trước khi thêm tính năng.
2. **Mỗi giai đoạn đều demo được:** không để giai đoạn nào chỉ có code chết.
3. **Rủi ro cao làm sớm:** realtime, offline sync, video call.
4. **Song song hoá:** chia việc backend/mobile theo feature để 2 người làm cùng lúc.

---

## 3. Các giai đoạn triển khai

### Phase 0 — Nghiên cứu & Chuẩn bị
**Mục tiêu:** chốt yêu cầu, kiến trúc, môi trường chạy được.
**Deliverables:**
- Bộ tài liệu SRS + kiến trúc (đã có) + mockup UI (Figma).
- Khảo sát công cụ tương tự (Trello, Asana, MS Teams) rút ra tính năng.
- Repo Android + Backend khởi tạo, docker-compose (PostgreSQL, Redis, MinIO).
- CI cơ bản (build + test) trên GitHub Actions.

**DoD:** `docker compose up` chạy được DB; app Android gọi được endpoint `GET /health` trả 200.

---

### Phase 1 — Nền tảng: Auth & User
**Mục tiêu:** đăng nhập, phân quyền thô, hạ tầng token.
**Deliverables (backend + android):**
- `users`, `refresh_tokens` (Flyway migration).
- Endpoint register/login/refresh/logout + JWT + BCrypt.
- Android: màn hình Splash/Login/Register/Forgot, lưu token an toàn, interceptor gắn token + auto-refresh, điều hướng theo trạng thái đăng nhập.
- Hồ sơ người dùng (xem/sửa).

**DoD:** đăng ký → đăng nhập → gọi API được bảo vệ thành công; token hết hạn tự refresh.
**Độ phức tạp:** M

---

### Phase 2 — Workspace, Dự án & Thành viên
**Deliverables:**
- Bảng `workspaces`, `workspace_members`, `projects`, `project_members`.
- API CRUD dự án, mời/thêm/xóa thành viên, vai trò RBAC.
- Android: danh sách dự án, chi tiết dự án, tab thành viên, màn tạo/sửa dự án.
- Gửi email lời mời (Spring Mail) — bản tối giản.

**DoD:** PM tạo dự án, mời thành viên; thành viên thấy dự án sau khi được thêm.
**Độ phức tạp:** M

---

### Phase 3 — Quản lý Task (lõi nghiệp vụ)
**Deliverables:**
- Bảng `tasks`, `task_assignees`, `labels`, `task_comments`, `subtasks`.
- API CRUD task, đổi trạng thái, gán nhiều người, lọc/sắp xếp/phân trang.
- Android: Kanban board (kéo thả), danh sách, chi tiết task, editor task, ưu tiên & nhãn.
- Lịch làm việc cá nhân (`schedule_events`) + hiển thị deadline.

**DoD:** tạo/gán task đầy đủ; board cập nhật đúng; lọc theo người/trạng thái/ưu tiên.
**Độ phức tạp:** L

---

### Phase 4 — Theo dõi tiến độ & Hiệu suất
**Deliverables:**
- Dashboard dự án: % hoàn thành, task theo trạng thái, task quá hạn.
- Bảng `time_entries` + timer start/stop + log thủ công.
- API tổng hợp thống kê năng suất theo ngày/tuần/tháng; trả dữ liệu biểu đồ.
- Android: màn thống kê, biểu đồ (Vico/MPAndroidChart), màn "Hôm nay" của tôi.

**DoD:** số liệu dashboard khớp dữ liệu task thật; timer hoạt động cả khi offline.
**Độ phức tạp:** M

---

### Phase 5 — Nhắc nhở & Thông báo
**Deliverables:**
- Bảng `notifications`, `devices`; `@Scheduled` quét deadline, tạo thông báo.
- Gửi FCM; lưu device token; trung tâm thông báo (đọc/chưa đọc).
- Thông báo sự kiện: được gán task, mention, đổi deadline, tin nhắn mới (khi offline).
- Android: NotificationService/Channel, deep-link khi bấm thông báo, cài đặt nhắc nhở.

**DoD:** đặt task deadline gần → nhận push đúng mốc (trước hạn + quá hạn), không trùng lặp.
**Độ phức tạp:** M

---

### Phase 6 — Realtime & Đồng bộ Offline
**Deliverables:**
- WebSocket STOMP: chat dự án, presence (Redis TTL), typing.
- Bảng `conversations`, `conversation_members`, `messages`.
- Android: màn chat, danh sách hội thoại, gửi ảnh/file, reply, trạng thái đã gửi/đã đọc.
- Room làm SSOT + `SyncWorker` (WorkManager) + endpoint `GET /sync?since=` + policy LWW.

**DoD:** 2 máy chat tức thời; tắt mạng vẫn tạo task được, có mạng tự đồng bộ không nhân bản dữ liệu.
**Độ phức tạp:** L

---

### Phase 7 — Hợp tác nâng cao: Video call & Tài liệu
**Deliverables:**
- Signaling WebSocket (offer/answer/ICE) + coturn (STUN/TURN); hoặc tích hợp Jitsi/Agora.
- Android: màn tạo/tham gia phòng họp, grid video, mute/camera/tắt cuộc gọi.
- Bảng `files` + MinIO upload/download; chia sẻ tài liệu vào dự án/task; phân quyền xem/sửa.

**DoD:** 2–4 thiết bị vào chung phòng họp được; upload file và mở lại được.
**Độ phức tạp:** L (rủi ro cao — xem mục 6)

---

### Phase 8 — Tích hợp, Bảo mật nâng cao & Hoàn thiện
**Deliverables:**
- Tích hợp Google Calendar (đồng bộ deadline, export .ics); thông báo email.
- RBAC hoàn chỉnh theo ma trận quyền; audit log; rate limit; 2FA (tùy chọn).
- Kiểm thử tổng thể, tối ưu hiệu năng, sửa bug.
- Tài liệu báo cáo đồ án, slide, kịch bản demo, dữ liệu mẫu.

**DoD:** vượt qua checklist nghiệm thu (mục 7); báo cáo hoàn chỉnh.
**Độ phức tạp:** M

---

## 4. Backlog ưu tiên (MoSCoW) & ánh xạ giai đoạn

| Ưu tiên | Chức năng (mã FR) | Giai đoạn |
|---|---|---|
| Must | FR-A01–A04 (Auth, hồ sơ) | P1 |
| Must | FR-B02, B03 (Dự án, thành viên) | P2 |
| Must | FR-B01, B04, B05, B06, B07 (Task, lịch, gán, ưu tiên, board) | P3 |
| Must | FR-C01, C02 (Dashboard tiến độ) | P4 |
| Must | FR-E01–E04 (Nhắc deadline, push, trung tâm thông báo) | P5 |
| Must | FR-D02, D03 (Chat nhóm realtime) | P6 |
| Must | FR-F01, F02 (Đồng bộ, offline) | P6 |
| Should | FR-C03, C04, C05 (Time tracking, thống kê) | P4 |
| Should | FR-D01, D05, D07 (Workspace, video, tài liệu) | P7 |
| Should | FR-E05, E06, FR-F03, F04 (Tùy chọn nhắc, email, calendar) | P5/P8 |
| Should | FR-G01, G02, G04, G05 (RBAC, phiên, audit) | P8 |
| Should | FR-A05, FR-D04, D08 (Quản lý thiết bị, presence, mention) | P6 |
| Could | FR-B08, B09, B10, FR-C06, FR-D06, FR-F05, FR-G06, FR-A06 | P8+ |
| Won't (giai đoạn này) | FR-F06 (Public API/Webhook) | — |

---

## 5. Kế hoạch kiểm thử

| Loại | Phạm vi | Công cụ |
|---|---|---|
| Unit test | Domain/UseCase, Service, mapper | JUnit5, Mockito, MockK |
| Integration test | Repository JPA, controller (MockMvc) | Testcontainers (PostgreSQL, Redis) |
| API test | Endpoint, auth, phân quyền (403/401) | RestAssured / Postman collection |
| UI test | Màn hình chính, luồng login→tạo task | Compose UI Test, Espresso |
| Realtime test | Chat 2 client, presence, mất kết nối | Test thủ công + script |
| Offline test | Tắt mạng, thao tác, bật lại | Test thủ công |
| Hiệu năng | Endpoint nóng (list task, chat) | k6/JMeter (cơ bản) |
| Bảo mật | Auth bypass, IDOR, injection cơ bản | OWASP ZAP (quét cơ bản) |

**Chiến lược:** test tập trung tầng domain/data (backend service + Android use case), không chạy đua coverage toàn bộ UI.

---

## 6. Rủi ro & Giảm thiểu

| # | Rủi ro | Mức | Giảm thiểu |
|---|---|---|---|
| R1 | WebRTC tự dựng phức tạp (NAT/TURN, đa thiết bị) | Cao | Làm Phase 7 muộn; fallback sang Jitsi/Agora SDK |
| R2 | Offline sync gây trùng/xung đột dữ liệu | Cao | UUID khoá chính, `version` + LWW, test kỹ Phase 6 |
| R3 | Realtime + FCM tốn thời gian debug | Vừa | Làm realtime chat trước, push sau; tách kênh rõ ràng |
| R4 | Ôm đồm quá nhiều chức năng | Cao | Bám MVP, cắt Could; giao diện tối giản |
| R5 | Hạ tầng deploy (VPS/domain/TLS) | Vừa | Docker Compose sẵn; dùng nền tảng free tier cho demo |
| R6 | Thành viên nhóm lệch tiến độ | Vừa | Chia feature theo module, review PR, milestone rõ |
| R7 | Thiết bị test hạn chế | Thấp | Dùng emulator + ít nhất 3 máy thật (2 Android, 1 iOS nếu có) |

---

## 7. Tiêu chí nghiệm thu / Demo

**Checklist demo cuối kỳ:**
- [ ] Đăng ký + đăng nhập 2 tài khoản, phân quyền PM/Member đúng.
- [ ] Tạo dự án, mời thành viên, thấy dự án trên máy thành viên.
- [ ] Tạo task có deadline/ưu tiên/nhãn, kéo thả Kanban, gán cho thành viên.
- [ ] Thành viên nhận push khi được gán task.
- [ ] Nhận nhắc nhở trước deadline; task quá hạn hiển thị đúng.
- [ ] Dashboard tiến độ + biểu đồ thống kê khớp dữ liệu.
- [ ] Chat nhóm realtime giữa 2 thiết bị; tạo task khi offline và đồng bộ khi có mạng.
- [ ] Họp video ≥ 2 thiết bị; chia sẻ tài liệu và mở lại được.
- [ ] Viewer không sửa được task (kiểm tra cả UI lẫn API).
- [ ] Báo cáo đồ án + slide + dữ liệu mẫu hoàn chỉnh.

---

## 8. Cấu trúc repo & Quy trình làm việc

```
MobileAppProject/
 ├─ android/            # App Kotlin (Gradle project)
 ├─ backend/            # Spring Boot (Maven project)
 ├─ docs/               # Tài liệu phân tích, kiến trúc, roadmap
 ├─ infra/              # docker-compose, nginx, coturn
 └─ README.md
```

**Git flow đề xuất:** `main` (ổn định) ← `develop` ← `feature/<ten-tinh-nang>`; PR bắt buộc review + CI pass.
**Quản lý công việc:** GitHub Projects/Trello, mỗi task gắn mã `FR-xx` để truy vết.
**Commit:** theo Conventional Commits (`feat:`, `fix:`, `docs:`...).

---

## 9. Việc cần chốt tiếp

1. **Deadline đồ án** → để quy đổi các giai đoạn trên thành mốc thời gian cụ thể.
2. **Số người trong nhóm** & phân công (backend/mobile/test).
3. **Phương án video call:** tự dựng WebRTC hay dùng Jitsi/Agora (ảnh hưởng Phase 7).
4. **Nền tảng deploy** cho demo (VPS riêng hay cloud free tier).
5. **Có làm bản iOS không** (hiện đang để tùy chọn).
