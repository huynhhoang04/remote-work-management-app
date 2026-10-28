# Hướng dẫn setup dịch vụ ngoài (Supabase Storage + Firebase FCM)

> Phase 2 dùng 2 dịch vụ ngoài tuỳ chọn:
> **Supabase Storage** (lưu file, thay cho đĩa local vì Render free không có persistent disk)
> và **Firebase Cloud Messaging** (push notification).
> Backend **vẫn chạy đầy đủ** khi chưa cấu hình: file dùng đĩa local, push chuyển no-op.
>
> Liên quan: `docs/plan-deploy/01-ci-cd-phase-1.md` (Render + Neon), `docs/plan-backend/04-tong-ket-trien-khai-backend.md` §5.

---

## 1. Chọn dịch vụ lưu file

| Dịch vụ | Free tier | Ưu | Nhược | Trạng thái trong dự án |
|---|---|---|---|---|
| **Supabase Storage** | 1 GB | REST đơn giản, bucket public, không cần SDK, tạo nhanh | 1 GB, phụ thuộc mạng | ✅ **Đã implement** (`SupabaseStorageService`) |
| Cloudinary | 25 GB | CDN ảnh, transform | API riêng, hợp ảnh hơn tài liệu | Chưa implement — cần thêm `StorageService` mới |
| Firebase Storage | 5 GB (project mới thường cần Blaze) | Cùng service account với FCM | Cần bật billing, thêm SDK `google-cloud-storage` | Chưa implement |
| MinIO (self-host) | — | S3 chuẩn, chủ động | Cần host + đĩa; Render free không có | Chưa implement — cần AWS SDK v2 |
| Đĩa local | — | Zero setup | Mất file mỗi lần Render redeploy/restart | ✅ Fallback dev/test (`LocalStorageService`) |

**Kết luận:** dùng Supabase cho demo/deploy; local cho dev/test. Muốn đổi sang Cloudinary/Firebase
Storage/MinIO chỉ cần viết thêm một implementation `StorageService` và đổi `STORAGE_TYPE`.
Các API `/files` không đổi.

---

## 2. Supabase Storage — từng bước

### 2.1 Tạo project và bucket

1. Đăng nhập <https://supabase.com> → **New project**.
   - Region: **Southeast Asia (Singapore)** (gần VN nhất).
   - Lưu `Database password` nếu dùng luôn Postgres của Supabase (dự án này **không** dùng —
     DB chính là Neon).
2. Chờ project khởi tạo xong → menu **Storage** → **New bucket**:
   - Name: `remoteapp-uploads`
   - Public bucket: **ON** (demo; xem cảnh báo bảo mật §6).
3. Menu **Project Settings → API**:
   - `Project URL`: `https://<project-ref>.supabase.co`
   - `service_role` key (mục **Project API keys**) — **secret**, không đưa xuống mobile.

### 2.2 Biến môi trường

Điền vào `.env` local hoặc dashboard Render:

```bash
STORAGE_TYPE=supabase
STORAGE_BASE_URL=https://<project-ref>.supabase.co
STORAGE_SERVICE_KEY=<service_role-key>
STORAGE_BUCKET=remoteapp-uploads
STORAGE_PUBLIC_URL=https://<project-ref>.supabase.co/storage/v1/object/public/remoteapp-uploads
```

### 2.3 Kiểm tra nhanh bằng cURL

```bash
# Upload
curl -X POST "$STORAGE_BASE_URL/storage/v1/object/$STORAGE_BUCKET/ping.txt" \
  -H "apikey: $STORAGE_SERVICE_KEY" \
  -H "Authorization: Bearer $STORAGE_SERVICE_KEY" \
  -H "Content-Type: text/plain" \
  --data-binary "ping"

# Đọc qua URL public
curl "$STORAGE_PUBLIC_URL/ping.txt"       # → ping
```

Nếu upload trả `400`/`401`: kiểm tra bucket name, key, và đã chọn đúng project.

### 2.4 Kiểm tra qua API backend

```bash
# 1. login lấy token
# 2. upload file gắn dự án
curl -X POST "http://localhost:8080/api/v1/files?projectId=<projectId>" \
  -H "Authorization: Bearer <accessToken>" \
  -F "file=@bruno/fixtures/sample.txt;type=text/plain"

# 3. tải lại
curl -H "Authorization: Bearer <accessToken>" \
  "http://localhost:8080/api/v1/files/<fileId>" -o /tmp/downloaded.txt
```

### 2.5 Ghi chú kỹ thuật

- Backend gọi Storage REST bằng `RestClient`; không thêm AWS SDK.
- Object key = `{uuid}.{ext}` (không dùng tên gốc) → URL khó đoán.
- `GET /files/{id}` **proxy** qua backend và kiểm quyền dự án, không redirect ra URL public.
  Tuy nhiên bucket public nghĩa là ai biết `STORAGE_PUBLIC_URL + key` vẫn đọc được — chấp nhận cho demo.
- Upload dùng header `x-upsert: true` để ghi đè nếu trùng key (thực tế key luôn mới).

---

## 3. Firebase Cloud Messaging — từng bước

### 3.1 Tạo project + service account

1. <https://console.firebase.google.com> → **Add project** (có thể tắt Google Analytics).
2. **Project settings** (bánh răng) → tab **Service accounts** → **Generate new private key**
   → tải file JSON.
3. Encode base64 (một dòng, không xuống dòng):

```bash
base64 -w0 service-account.json > service-account.b64
# macOS: base64 -i service-account.json | tr -d '\n' > service-account.b64
```

4. Đặt env (local hoặc Render):

```bash
FCM_ENABLED=true
FCM_CREDENTIALS_BASE64=<nội dung file .b64>
```

### 3.2 Thêm Android app (chuẩn bị cho phase mobile)

- Firebase Console → **Add app** → Android → package name của app Expo/Kotlin.
- Tải `google-services.json` cho mobile (không commit vào repo backend).
- Mobile lấy FCM token và gọi `POST /api/v1/devices/token`.

### 3.3 Kiểm tra

1. `POST /api/v1/devices/token` với token thật hoặc token giả.
2. Gán task cho user đó (`PATCH /tasks/{id}/assignees` hoặc tạo task có assignee).
3. Xem log backend:
   - `FCM_ENABLED=true` + credentials đúng → `FcmPushSender` gửi multicast.
   - Thiếu credentials → log `Không khởi tạo được Firebase — push bị tắt`.
   - `FCM_ENABLED=false` → log `[FCM disabled] Bỏ qua push ...`.
4. Trong Firebase Console → **Messaging** có thể gửi test tới token đã đăng ký.
5. Token `UNREGISTERED`/`INVALID_ARGUMENT` → backend tự xoá khỏi bảng `devices`.

### 3.4 Lưu ý

- `firebase-admin` khởi tạo **lazy**, lỗi credentials không làm sập app.
- Service account JSON chứa private key — chỉ nằm trong env, **không commit**.
- Một Firebase project dùng được cho cả Android app và backend.

---

## 4. Bảng biến môi trường

| Biến | Local | Render | Secret | Ghi chú |
|---|---|---|---|---|
| `STORAGE_TYPE` | `local` | `supabase` | Không | `local` không cần key |
| `STORAGE_BASE_URL` | — | `https://<ref>.supabase.co` | Không | |
| `STORAGE_SERVICE_KEY` | — | service_role key | **Có** | Không đưa xuống mobile |
| `STORAGE_BUCKET` | — | `remoteapp-uploads` | Không | |
| `STORAGE_PUBLIC_URL` | — | URL public bucket | Không | Dùng cho verify/tài liệu |
| `UPLOAD_DIR` | `uploads` | — | Không | Chỉ khi `local` |
| `FCM_ENABLED` | `false` | `true` (nếu có credentials) | Không | |
| `FCM_CREDENTIALS_BASE64` | — | nội dung .b64 | **Có** | |
| `APP_TIMEZONE` | `Asia/Ho_Chi_Minh` | `Asia/Ho_Chi_Minh` | Không | |
| `REMINDER_INTERVAL_MS` | `300000` | `300000` | Không | 5 phút |
| `REMINDER_INITIAL_DELAY_MS` | `60000` | `60000` | Không | Hoãn lần quét đầu |

`render.yaml` đã khai báo các biến này với `sync: false` (Render hỏi giá trị lúc tạo, không lưu repo).

---

## 5. Checklist setup

### Supabase

- [ ] Tạo project region Singapore.
- [ ] Bucket `remoteapp-uploads` ở chế độ Public.
- [ ] Lấy `Project URL` + `service_role` key.
- [ ] Điền 5 biến `STORAGE_*` vào Render (và `.env` local nếu muốn test thật).
- [ ] Upload/đọc thử `ping.txt` bằng cURL (§2.3).
- [ ] Upload qua `POST /api/v1/files` rồi `GET /api/v1/files/{id}` thành công.

### Firebase

- [ ] Tạo Firebase project.
- [ ] Generate service account JSON.
- [ ] Base64 một dòng, điền `FCM_ENABLED` + `FCM_CREDENTIALS_BASE64`.
- [ ] Đăng ký device token qua API.
- [ ] Gán task → thấy log gửi push / nhận notification.
- [ ] Thêm Android app + `google-services.json` khi làm mobile.

### Chung

- [ ] Không commit file JSON/key/base64.
- [ ] `curl https://<service>.onrender.com/actuator/health` = `UP`.
- [ ] Flyway đã chạy `V6` trên Neon (Render logs).
- [ ] Bruno `00-smoke` xanh trên `render.yml`.
- [ ] Nếu đổi key: rotate trên dashboard + cập nhật Render env + redeploy.

---

## 6. Bảo mật

| Rủi ro | Cách xử lý |
|---|---|
| Lộ `service_role` key | Chỉ đặt trong env Render; không log; rotate ngay nếu lộ |
| Lộ service account JSON | Coi như lộ quyền gửi push; revoke key trong Firebase Console |
| Bucket public bị đoán URL | Key là UUID; không đặt tên file gốc; production nên chuyển bucket private + signed URL |
| File độc hại | Whitelist MIME + giới hạn 10MB (attachment) / 5MB (avatar); không thực thi file |
| File nhạy cảm | Không dùng bucket public cho dữ liệu thật; thêm signed URL khi cần |
| Tải file hộ | `GET /files/{id}` kiểm tra thành viên dự án; avatar chỉ cần đăng nhập |

---

## 7. Troubleshooting

| Triệu chứng | Nguyên nhân thường gặp | Cách xử lý |
|---|---|---|
| Upload trả 500 `STORAGE_ERROR` | Sai `STORAGE_BASE_URL`/bucket/key; bucket chưa tạo | Kiểm tra lại §2.1–2.2, chạy cURL §2.3 |
| Upload trả 401/403 | Dùng `anon` key thay vì `service_role` | Lấy lại key đúng |
| `GET /files/{id}` 404 dù vừa upload | Đổi cấu hình storage sau khi upload; object không còn | Upload lại; kiểm tra `files.storage_key` |
| File mất sau redeploy | `STORAGE_TYPE=local` trên Render (không có đĩa) | Chuyển sang Supabase |
| Upload 10MB bị chặn | `spring.servlet.multipart.max-file-size=10MB` | Giảm file hoặc tăng config |
| Avatar báo sai loại | Chỉ nhận jpg/png/gif/webp | Đổi định dạng ảnh |
| Push không tới | `FCM_ENABLED=false`, thiếu credentials, token cũ | Xem log `FcmPushSender`, kiểm tra env, đăng ký lại token |
| Log `Không khởi tạo được Firebase` | Base64 hỏng/xuống dòng, JSON sai project | Encode lại `base64 -w0`, kiểm tra file JSON |
| Không có notification nhắc deadline | Scheduler mới chạy sau 60s; cửa sổ 1 ngày/1 giờ; task không deadline | Chờ, kiểm tra `deadline`, xem log |
| Nhắc bị trùng | `dedupe_key` bị NULL do code cũ | Kiểm tra version; dedupe là UNIQUE partial index |
| Neon chậm/lỗi connection | Autosuspend/giới hạn pool | Giữ `DB_POOL_SIZE=5`, dùng host direct (xem plan deploy 01) |
| Render cold start | Free tier ngủ 15 phút | Client timeout ≥ 60s; UptimeRobot ping health |

---

## 8. Fallback matrix

| Thiếu cấu hình | Hành vi |
|---|---|
| Không có `STORAGE_*` | `LocalStorageService` ghi vào `UPLOAD_DIR` (mất file khi Render restart) |
| `STORAGE_TYPE=supabase` nhưng thiếu key | App **không khởi động** (`SupabaseStorageService` ném `STORAGE_ERROR`) — cố ý fail-fast |
| `FCM_ENABLED=false` | `NoopPushSender`: notification vẫn lưu DB, WS vẫn gửi, chỉ không push |
| `FCM_ENABLED=true` nhưng thiếu/sai credentials | Log lỗi, `FcmPushSender` chuyển no-op; app vẫn chạy |
| Thiếu cả storage + FCM | Backend vẫn đầy đủ chức năng trừ file bền vững và push |

---

## 9. Lệnh kiểm tra tổng hợp

```bash
# Health
curl -s https://<service>.onrender.com/actuator/health

# Flyway đã chạy tới V6 (Neon SQL editor)
select version, description, success from flyway_schema_history order by installed_rank;

# Số file đã lưu metadata
select count(*) from files;

# Device token đã đăng ký
select user_id, platform, last_active_at from devices order by created_at desc limit 5;
```
