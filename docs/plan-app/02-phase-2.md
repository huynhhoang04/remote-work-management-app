# Plan App 02 — React Native phase 2 (đầy đủ tính năng còn lại)

> Nối tiếp `01-phase-1.md` (auth, dự án, thành viên, link mời, hồ sơ).
> Backend phase 2 đã xong và deploy (`docs/plan-backend/04-tong-ket-trien-khai-backend.md`).
> Mục tiêu phase 2: **toàn bộ tính năng còn lại** trên app, dùng đúng 65 endpoint + 2 destination STOMP.
> Phase 3 mới là sửa lỗi/tinh chỉnh, phase 4 viết tài liệu.

---

## 1. Phạm vi phase 2

| Nhóm | Chức năng | API dùng |
|---|---|---|
| Task | Kanban 4 cột, danh sách, tạo/sửa/xoá, gán người, nhãn, checklist, bình luận, đính kèm | §5 (26 endpoint) |
| Task | Kéo thả trạng thái + vị trí (`position`) | `PATCH /tasks/{id}/position`, `/status` |
| Lịch | Xem tháng + danh sách sự kiện theo ngày, tạo/sửa/xoá, liên kết task | `GET/POST /schedule/events`, `PATCH/DELETE /schedule/events/{id}` |
| Thống kê | "Hôm nay" 4 nhóm, năng suất cá nhân, dashboard dự án | `GET /me/today`, `/me/stats`, `/projects/{id}/stats` |
| Thông báo | Trung tâm, chưa đọc, đánh dấu đã đọc, deep-link, badge tab | `GET /notifications`, `/unread-count`, `PATCH /{id}/read`, `/read-all` |
| Chat | Danh sách hội thoại GROUP/DIRECT, lịch sử phân trang, gửi tin realtime + fallback REST, reply, read receipt | §8 + STOMP |
| Dự án | Sửa/xoá dự án, đổi vai trò/xoá thành viên, audit log (Owner) | §4 |
| Hồ sơ | Upload avatar, đăng ký/huỷ FCM device token | `POST /users/me/avatar`, `/devices/token` |
| Push | Xin quyền, lấy token, đăng ký backend, deep-link khi bấm; no-op nếu thiếu cấu hình FCM | `expo-notifications` |

**Không làm trong phase 2** (giữ đúng scope đã cắt): typing/presence, gọi video, offline sync hai chiều,
import/export, i18n đa ngôn ngữ. Chat chưa gửi file trực tiếp (backend chưa có endpoint gắn `messageId`)
— nếu cần sẽ làm ở phase 3.

---

## 2. Điều hướng & cấu trúc màn hình

Chuyển từ stack đơn sang **bottom tabs** (5 tab) + stack chi tiết:

```
/                         splash → /today hoặc /login
/(auth)/login|register|forgot-password
/(app)/(tabs)
  ├─ today                 "Hôm nay" (dashboard + lối vào lịch)
  ├─ projects              danh sách dự án
  ├─ chat                  hội thoại
  ├─ notifications         trung tâm thông báo (badge chưa đọc)
  └─ profile               hồ sơ + menu (lịch, thống kê, đăng xuất)
/(app)/projects/new|join
/(app)/projects/[id]       tổng quan dự án (mô tả, thành viên rút gọn, lối tắt)
/(app)/projects/[id]/kanban
/(app)/projects/[id]/members
/(app)/projects/[id]/stats
/(app)/projects/[id]/labels
/(app)/projects/[id]/audit
/(app)/projects/[id]/settings
/(app)/tasks/new?projectId=
/(app)/tasks/[id]          chi tiết task (subtask, comment, file)
/(app)/tasks/[id]/edit
/(app)/schedule            lịch tháng + agenda
/(app)/schedule/new        tạo sự kiện (modal)
/(app)/schedule/[id]       sửa sự kiện
/(app)/stats               thống kê cá nhân
/(app)/chat/[id]           phòng chat
```

- Tab "Chat"/"Thông báo" có badge số chưa đọc (query + STOMP cập nhật cache).
- Deep-link từ thông báo: `refType=TASK` → `/tasks/{refId}`; `MESSAGE` → `/chat/{refId}`.

---

## 3. Quyết định kỹ thuật

| Hạng mục | Quyết định |
|---|---|
| Token/HTTP | Giữ `api/client.ts` (interceptor refresh single-flight). Mọi API mới đi qua đây |
| Server state | TanStack Query; query key theo resource, invalidate chéo (`tasks`, `project stats`, `today`) |
| Chat realtime | `@stomp/stompjs` + `WebSocket` native, token qua query `?token=`; reconnect 5s; REST fallback khi WS chưa nối |
| TextEncoder | Polyfill UTF-8 nhỏ trong `src/utils/polyfills.ts` (Hermes chưa có) |
| File | `expo-document-picker` + `expo-image-picker`; upload multipart qua axios; tải file bằng `File.downloadFileAsync` kèm Bearer rồi `expo-sharing` |
| Ảnh cần auth | `<Image source={{ uri, headers: { Authorization } }} />` qua helper `authImageSource` |
| Push | `expo-notifications` + `expo-device`; nếu thiếu `google-services.json`/quyền → bỏ qua, không chặn app |
| Kanban | 4 cột ngang; kéo thả bằng `PanResponder` + đo layout cột, thả ra ngoài = huỷ; fallback menu "Chuyển trạng thái" |
| Form | react-hook-form + zod, schema khớp DTO backend |
| Ngày giờ | `dayjs` hiển thị + `@react-native-community/datetimepicker` chọn |
| Biểu đồ | Thanh ngang/dọc dựng bằng `View` + token (không thêm thư viện chart) |
| UI | Tiếp tục Paper M3 + tokens; mọi text mới thêm vào `STR` |

---

## 4. Tầng dữ liệu

### 4.1 Types (`src/types/api.ts`)

Thêm: `TaskStatus`, `TaskPriority`, `LabelDto`, `TaskAssigneeDto`, `SubtaskDto`, `FileDto`, `TaskDto`,
`CommentDto`, `ScheduleEventDto`, `TaskBriefDto`, `EventBriefDto`, `TodayDto`, `CountPointDto`,
`MyStatsDto`, `MemberStatDto`, `ProjectStatsDto`, `NotificationDto`, `ConversationDto`,
`MessageDto`, `ReadReceiptDto`, `AuditLogDto`. Giữ nguyên DTO phase 1.

### 4.2 API modules

```
src/api/tasks.ts          list/create/get/update/delete/status/position/assignees/labels
                          subtasks/comments/attachments
src/api/files.ts          upload(projectId?), uploadAvatar, downloadAndShare, authImageSource
src/api/schedule.ts       list/create/update/delete
src/api/stats.ts          today/myStats/projectStats
src/api/notifications.ts  list/unreadCount/markRead/markAllRead
src/api/chat.ts           conversations/createDirect/messages/send/read
src/api/devices.ts        register/unregister
src/api/ws.ts             STOMP client dùng chung (chat + notification)
```

`src/api/projects.ts` bổ sung: `update`, `remove`, `changeMemberRole`, `removeMember`, `auditLogs`.

### 4.3 Schemas

`schemas/task.ts` (title ≤ 200, description ≤ 5000, deadline ≥ startDate, comment ≤ 2000),
`schemas/schedule.ts` (title ≤ 200, end > start), `schemas/chat.ts` (content 1–4000),
`schemas/project.ts` thêm `editProjectSchema`, `labelSchema`, `inviteExpiry?`.

---

## 5. Màn hình & hành vi

### 5.1 Tab "Hôm nay"
- Lời chào + tên; 4 thẻ số liệu (quá hạn, hôm nay, 7 ngày tới, sự kiện hôm nay).
- Section **Quá hạn** / **Đến hạn hôm nay** / **Sắp tới** (TaskBriefCard → task detail).
- Section **Lịch hôm nay** (giờ + tiêu đề, tap → sửa sự kiện); nút "Mở lịch".
- Pull-to-refresh; loading skeleton; empty "Hôm nay bạn không có việc nào"; error retry.

### 5.2 Tab "Dự án"
- Giữ danh sách + tạo + join; header có icon thông báo? (đã có tab) — thêm icon Today không cần.
- Card dự án hiện thêm tiến độ? Không có trong DTO list → chỉ hiển thị như phase 1.

### 5.3 Chi tiết dự án
- Mô tả + `RoleChip`.
- Grid lối tắt: **Bảng công việc**, **Thành viên**, **Thống kê**, **Nhãn** (Member+), **Cài đặt** (Owner/Admin),
  **Nhật ký** (Owner).
- Danh sách thành viên rút gọn + `InviteCard`.
- Owner/Admin: sửa/xoá dự án trong Cài đặt.

### 5.4 Kanban & Task
- Header: tên dự án, ô tìm kiếm, chip lọc (trạng thái/ưu tiên/người/nhãn), nút đổi chế độ Bảng/Danh sách.
- Kanban: 4 cột TODO/DOING/REVIEW/DONE, cuộn ngang; card hiện tiêu đề, nhãn, avatar assignee, deadline
  (đỏ nếu quá hạn), priority; nhấn → chi tiết; **kéo thả** đổi cột (gọi `position` = max+1000 của cột đích,
  optimistic update).
- Danh sách: nhóm theo trạng thái, sắp theo deadline/priority.
- FAB tạo task.
- Task detail: tiêu đề, status/priority chip (đổi nhanh), mô tả, ngày, nhãn, assignee (dialog chọn nhiều),
  checklist (thêm/đánh dấu/xoá), đính kèm (chọn file/ảnh → upload, mở/chia sẻ, xoá), bình luận
  (thêm, xoá của mình, @mention chọn thành viên), nút sửa/xoá.
- Xử lý `TASK_VERSION_CONFLICT`: báo "Dữ liệu vừa thay đổi, đã tải lại" + refetch.
- Viewer: chỉ đọc (ẩn FAB và hành động ghi).

### 5.5 Lịch
- Month grid (dayjs), chấm sự kiện theo ngày; chọn ngày → agenda.
- FAB tạo sự kiện; sự kiện có `taskId` hiện liên kết.
- Form: tiêu đề, mô tả, bắt đầu/kết thúc (date + time picker), chọn task liên kết (tuỳ chọn).
- Chỉ chủ sự kiện thấy nút sửa/xoá.

### 5.6 Thống kê
- Cá nhân: chọn khoảng 7/30/90 ngày; biểu đồ cột hoàn thành theo ngày/tuần/tháng (segmented);
  thẻ tỉ lệ đúng hạn.
- Dự án: tổng task, % hoàn thành (progress), theo trạng thái (bars), quá hạn, bảng theo thành viên.

### 5.7 Thông báo
- FlatList phân trang vô hạn; icon theo `type`; chưa đọc = nền nhấn + chấm; tap = mark read + deep-link.
- "Đọc tất cả" ở header; empty/error/loading.
- Đăng ký STOMP `/user/queue/notifications` khi app mở → prepend cache + tăng badge.

### 5.8 Chat
- Danh sách: avatar/tên, tin cuối (rút gọn), thời gian, badge unread; GROUP có icon dự án.
- Nút tạo chat 1-1: chọn thành viên từ một dự án (dialog).
- Phòng chat: header tên + thành viên (GROUP); lịch sử phân trang ngược (cuộn lên tải thêm);
  bubble trái/phải, tên người gửi (GROUP), reply preview, thời gian; input + gửi.
- Gửi: nếu WS connected → `/app/chat.send`; nếu không → `POST /messages` (cùng `clientMessageId`).
- Nhận: subscribe `/topic/conversations.{id}`; cập nhật cache, đánh dấu đã đọc khi màn hình active.
- Read receipt broadcast → hiện "Đã đọc" cho tin của mình (DIRECT).
- Empty "Chưa có tin nhắn"; lỗi gửi → giữ tin ở trạng thái lỗi + gửi lại.

### 5.9 Hồ sơ
- Thêm upload avatar (camera/thư viện), hiển thị avatar URL có Bearer.
- Menu: Lịch của tôi, Thống kê, (Thông báo đã có tab), Đăng xuất.
- Đăng ký FCM token sau đăng nhập; huỷ token trước khi logout (best effort).

### 5.10 Push notification
- `registerForPushNotifications()`: xin quyền, `getDevicePushTokenAsync`, `POST /devices/token`.
- Android cần `google-services.json` (hướng dẫn README); thiếu → log + bỏ qua.
- Bấm thông báo → điều hướng theo `refType/refId` (dữ liệu trong `notification.request.content.data`).

---

## 6. Thứ tự thực hiện

1. Foundation: types, API modules, schemas, polyfills, helper file/date, mở rộng STR.
2. Điều hướng: tabs + stack chi tiết, splash/login redirect `/today`, badge.
3. Task: API/hooks/components + Kanban + form + chi tiết (subtask/comment/file).
4. Chi tiết dự án mở rộng: members (đổi vai trò/xoá), labels, settings, stats, audit.
5. Lịch + "Hôm nay" + Thống kê cá nhân.
6. Thông báo (center + badge + deep-link).
7. Chat (REST + STOMP + read receipt).
8. Avatar + FCM device token + push handler.
9. Test: schema, helper format, WS frame parser, component KanbanCard/MessageBubble/NotificationRow.
10. `npx tsc --noEmit` + `npm test` + cập nhật README/AGENTS.

---

## 7. Definition of Done (phase 2 app)

- [ ] `npx tsc --noEmit` và `npm test` xanh.
- [ ] Đăng nhập → tab Hôm nay hiển thị đúng 4 nhóm; điều hướng chéo hoạt động.
- [ ] Tạo task từ Kanban; kéo thả đổi cột; sửa task; conflict version báo đúng.
- [ ] Gán người/nhãn, checklist, bình luận + mention, upload/mở/xoá đính kèm.
- [ ] Lịch: tạo/sửa/xoá sự kiện, xem theo tháng/ngày.
- [ ] Thống kê cá nhân + dashboard dự án có số liệu thật.
- [ ] Thông báo: badge, đọc 1/tất cả, deep-link task/chat.
- [ ] Chat: 2 tài khoản nhắn realtime, lịch sử phân trang, read receipt, fallback REST khi mất WS.
- [ ] Đổi vai trò/xoá thành viên, sửa/xoá dự án, audit log (Owner).
- [ ] Avatar upload hiển thị được (ảnh cần Bearer).
- [ ] Push: đăng ký token khi có cấu hình; thiếu cấu hình không crash.

---

## 8. Rủi ro & giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| STOMP/TextEncoder không có trên Hermes | Polyfill UTF-8 + test frame parser; REST fallback |
| Render free cold start khi mở chat | timeout 60s, reconnect backoff, banner sẵn có |
| Kéo thả Kanban phức tạp | PanResponder + đo layout; fallback menu đổi trạng thái |
| FCM cần google-services.json | Guard try/catch, tài liệu hoá, notification center vẫn đủ |
| File cần Bearer | download kèm header + sharing; ảnh dùng Image headers |
| Backend trả version conflict | Refetch + thông báo, không mất dữ liệu người dùng |

---

## 9. Trạng thái triển khai (đã xong code)

| Hạng mục | Kết quả |
|---|---|
| Typecheck | `npx tsc --noEmit` ✅ |
| Test | 10 suite / **43 test** xanh (schema, error map, StateView, TaskChips, MessageBubble, UTF-8 polyfill, format, chat cache) |
| Bundle | `npx expo export --platform android` ✅ (2223 modules) |
| Navigation | Bottom tabs Hôm nay · Dự án · Chat · Thông báo · Hồ sơ + stack chi tiết |
| Task | Kanban kéo thả, danh sách, lọc, form tạo/sửa, chi tiết (checklist, comment/mention, file) |
| Lịch | Lưới tháng + agenda, tạo/sửa/xoá, liên kết task |
| Thống kê | Cá nhân (7/30/90 ngày) + dashboard dự án |
| Thông báo | Trung tâm + badge + đọc 1/tất cả + deep-link + STOMP |
| Chat | GROUP/DIRECT, phân trang, gửi STOMP + fallback REST, reply, read receipt |
| Dự án | Sửa/xoá, đổi vai trò/gỡ thành viên, nhãn, audit log |
| Hồ sơ | Avatar (Bearer image), lối vào lịch/thống kê, FCM token (best effort) |

### 9.1 Bản đồ file chính

```
src/api/            tasks.ts files.ts schedule.ts stats.ts notifications.ts chat.ts devices.ts ws.ts
src/types/api.ts    toàn bộ DTO phase 2
src/features/       tasks/hooks.ts + TaskForm.tsx, schedule/hooks.ts + ScheduleEventForm.tsx,
                    stats/hooks.ts, notifications/hooks.ts + RealtimeBridge.tsx,
                    chat/hooks.ts, push/push.ts
src/components/     TaskCard, KanbanBoard, TaskChips, AssigneeAvatars, TaskBriefCard,
                    NotificationRow, ConversationRow, MessageBubble, DateTimeField,
                    MultiSelectDialog, ChoiceRow, TabHeader
src/app/(app)/(tabs)/  today.tsx projects.tsx chat.tsx notifications.tsx profile.tsx
src/app/(app)/      projects/[id]/{index,kanban,members,stats,labels,settings,audit}.tsx
                    tasks/new.tsx tasks/[id]/{index,edit}.tsx
                    schedule/{index,new,[id]}.tsx chat/{new,[id]}.tsx stats.tsx
```

### 9.2 Hạn chế / phase 3

1. **Push FCM** cần `google-services.json` + backend `FCM_ENABLED=true`; chưa cấu hình thì app
   vẫn dùng trung tâm thông báo (chỉ thiếu push).
2. **Kanban kéo thả** dùng cử chỉ long-press + đo layout cột; nếu thiết bị/gesture không ổn,
   vẫn đổi trạng thái trong chi tiết task.
3. **Chat chưa gửi file trực tiếp** — backend chưa có endpoint gắn `messageId` (xem
   `docs/plan-backend/04` §8.1); có thể upload rồi gửi URL ở phase 3.
4. **Read receipt** chỉ hiển thị "Đã đọc" khi nhận broadcast trong phiên hiện tại.
5. **Lịch** tải sự kiện theo tháng hiển thị; màn sửa sự kiện quét khoảng ±2 năm vì backend
   không có endpoint lấy 1 sự kiện.
6. **Attachment delete**: UI hiện nút cho Member+; backend chỉ cho uploader/Owner/Admin → có thể
   trả 403, phase 3 sẽ ẩn theo `ownerId` nếu bổ sung DTO.
7. Chưa có QA trên máy thật/emulator trong môi trường CI — phase 3 chạy checklist thủ công
   (đăng nhập 2 tài khoản, chat realtime, Render cold start, upload file).

### Sửa lỗi QA ngày 26/11/2026

- Nhật ký hoạt động dùng `actorName` từ API, nhãn loại đối tượng tiếng Việt và nội dung metadata đã diễn giải. Bản ghi hệ thống hoặc API cũ thiếu tên có nhãn dự phòng.
- Bộ lọc đổi vai trò dùng đúng mã `ROLE_CHANGED` của backend.
