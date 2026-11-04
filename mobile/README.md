# RemoteApp Mobile — React Native (Expo + TypeScript)

Ứng dụng Android cho đồ án **Quản lý & theo dõi làm việc từ xa** (đề tài 21).
Backend: Spring Boot đã deploy — mặc định `https://remoteapp-backend.onrender.com`.

> Tài liệu chi tiết: [`docs/plan-app/01-phase-1.md`](../docs/plan-app/01-phase-1.md) ·
> [`docs/plan-app/02-phase-2.md`](../docs/plan-app/02-phase-2.md)

## Tính năng

**Phase 1** — đăng ký/đăng nhập/refresh token, quên mật khẩu, hồ sơ, dự án, thành viên,
link mời, phân quyền UI theo vai trò.

**Phase 2** — toàn bộ tính năng còn lại:

- **Hôm nay**: task quá hạn / đến hạn / 7 ngày tới + sự kiện trong ngày.
- **Công việc**: Kanban 4 cột kéo thả, danh sách, tìm kiếm/lọc, tạo/sửa/xoá, giao người,
  nhãn, checklist, bình luận + @mention, đính kèm file/ảnh.
- **Lịch cá nhân**: lưới tháng + agenda, sự kiện liên kết task.
- **Thống kê**: năng suất cá nhân (7/30/90 ngày, đúng hạn) + dashboard tiến độ dự án.
- **Thông báo**: trung tâm, badge, đọc 1/tất cả, deep-link; push FCM nếu cấu hình.
- **Chat**: hội thoại GROUP (dự án) + DIRECT, lịch sử phân trang, STOMP realtime
  (fallback REST), trả lời, read receipt.
- **Quản trị dự án**: sửa/xoá dự án, đổi vai trò/gỡ thành viên, nhãn, audit log (Owner).
- **Hồ sơ**: đổi avatar, đăng ký FCM device token, đăng xuất.

## Chạy app

```bash
cd mobile
npm ci
cp .env.example .env          # EXPO_PUBLIC_API_URL (mặc định URL Render)
npx expo start                # Expo Go / emulator
npx tsc --noEmit              # typecheck
npm test                      # Jest + RNTL
npx expo run:android          # build APK local (Android Studio/SDK)
```

`EXPO_PUBLIC_API_URL` trỏ backend local khi dev: `http://<LAN-IP>:8080` (điện thoại thật
không dùng được `localhost`).

### Native modules mới (phase 2)

Phase 2 thêm: `expo-notifications`, `expo-device`, `expo-image-picker`,
`expo-document-picker`, `expo-file-system`, `expo-sharing`,
`@react-native-community/datetimepicker`, `@stomp/stompjs`.
Sau khi `npm ci` cần **rebuild native** (`npx expo run:android` hoặc EAS Build) — không dùng
Expo Go cho các tính năng chọn tệp/thông báo.

### Push notification (tuỳ chọn)

- Cần `google-services.json` (Firebase Android app `com.example.remoteapp`) đặt trong `mobile/`
  và bật `FCM_ENABLED=true` + `FCM_CREDENTIALS_BASE64` ở backend
  ([hướng dẫn](../docs/plan-deploy/02-dich-vu-ngoai.md)).
- Thiếu cấu hình: app vẫn chạy, trung tâm thông báo đầy đủ, chỉ không có push.

### Chat realtime

- WebSocket tại `/ws`, STOMP destination `/topic/conversations.{id}` và `/user/queue/notifications`.
- `src/api/ws.ts` tự reconnect 5 giây; khi mất kết nối, gửi tin qua REST (`POST /messages`)
  với `clientMessageId` để chống trùng.

## Cấu trúc

```
src/
├─ app/                 # Expo Router: (auth), (app)/(tabs), chi tiết dự án/task/lịch/chat
├─ api/                 # axios client + refresh, tasks, files, schedule, stats, notifications, chat, ws
├─ components/          # Paper M3 components dùng chung (TaskCard, KanbanBoard, MessageBubble…)
├─ features/            # hooks TanStack Query + TaskForm, ScheduleEventForm, RealtimeBridge
├─ schemas/             # zod schema khớp DTO backend
├─ store/               # SessionProvider, QueryClient
├─ theme/               # tokens + Paper theme
└─ utils/               # constants (text VI), format, pickers, polyfills, id
```

## Kiểm thử

```bash
npm test
```

Hiện có 10 suite / 43 test: zod schema (auth/task/schedule), map lỗi API, StateView, TaskChips, MessageBubble,
UTF-8 polyfill cho STOMP, format helper và cache tin nhắn (optimistic/idempotent).

## Quy ước

- TypeScript strict; server state **chỉ** qua TanStack Query; HTTP **chỉ** qua `src/api/client.ts`.
- Mọi text tiếng Việt trong `src/utils/constants.ts`; màu/spacing lấy từ `src/theme/tokens.ts`.
- Token lưu `expo-secure-store`; không commit `.env`, `node_modules`, `.expo`.
