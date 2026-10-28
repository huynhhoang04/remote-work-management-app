# Deploy phase 2 — backend đầy đủ (task/stats/chat/file)

> Phase 1 đã có: Render (Docker, free) + Neon (PostgreSQL) + GitHub Actions + Deploy Hook.
> Phase 2 thêm: Flyway `V3`–`V6` (task, lịch, chat, thông báo, file) + **Supabase Storage** (tuỳ chọn)
> + **Firebase FCM** (tuỳ chọn). Hướng dẫn dịch vụ ngoài chi tiết: `02-dich-vu-ngoai.md`.

---

## 1. Cần đăng ký gì?

| Dịch vụ | Bắt buộc? | Dùng để | Ghi chú |
|---|---|---|---|
| GitHub | ✅ đã có | CI + trigger deploy | repo hiện tại |
| Render | ✅ đã có | chạy backend Docker | service `remoteapp-backend` |
| Neon | ✅ đã có | PostgreSQL | Flyway tự chạy V3–V6 |
| **Supabase** | ⚠️ **nên có** | lưu file bền vững | Render free không có đĩa; không có Supabase thì file mất khi redeploy |
| **Firebase** | ❌ không bắt buộc | push FCM | có thể deploy với `FCM_ENABLED=false`, bật sau |
| Cloudinary/Firebase Storage/MinIO | ❌ không cần | thay Supabase | chỉ khi muốn đổi backend storage |

**Chốt:** để deploy ngay, bạn **chỉ cần Supabase** (nếu muốn upload file hoạt động đúng).
Firebase để sau khi làm mobile, vì chưa có app thì cũng chưa có FCM token thật.

---

## 2. Thứ tự deploy (không được đảo)

1. Tạo Supabase bucket + lấy key.
2. Điền env mới trên Render (chưa bật `STORAGE_TYPE=supabase` nếu key chưa sẵn).
3. Commit + push `main`.
4. CI chạy test → build Docker → gọi Render Deploy Hook.
5. Render chạy Flyway `V3`–`V6` trên Neon → healthcheck.
6. Smoke + kiểm tra upload file.
7. (Sau) bật Firebase FCM khi có app/token.

> **Quan trọng:** đừng đặt `STORAGE_TYPE=supabase` trước khi có đủ `STORAGE_BASE_URL` +
> `STORAGE_SERVICE_KEY`; app fail-fast nếu thiếu. Để trống → tự fallback local (an toàn).

---

## 3. Supabase Storage (nếu muốn file bền vững)

### 3.1 Tạo

1. <https://supabase.com> → **New project** → region **Southeast Asia (Singapore)**.
2. **Storage** → **New bucket**:
   - Name: `remoteapp-uploads`
   - Public bucket: **ON** (demo).
3. **Project Settings → API** → copy:
   - `Project URL` → `STORAGE_BASE_URL`
   - `service_role` key → `STORAGE_SERVICE_KEY` (**secret**)

### 3.2 Biến trên Render

Vào Render → service `remoteapp-backend` → **Environment** → thêm:

| Key | Value |
|---|---|
| `STORAGE_TYPE` | `supabase` |
| `STORAGE_BASE_URL` | `https://<project-ref>.supabase.co` |
| `STORAGE_SERVICE_KEY` | `<service_role-key>` (secret) |
| `STORAGE_BUCKET` | `remoteapp-uploads` |
| `STORAGE_PUBLIC_URL` | `https://<project-ref>.supabase.co/storage/v1/object/public/remoteapp-uploads` |

`render.yaml` đã khai báo các key này với `sync: false`; nếu Render không tự thêm vào service
đang chạy, thêm tay như bảng trên.

### 3.3 Vì sao không dùng đĩa local trên Render?

Render free không có persistent disk → container restart/redeploy là mất `uploads/`.
Chỉ dùng `STORAGE_TYPE=local` khi dev local.

---

## 4. Firebase FCM (tuỳ chọn, làm sau cũng được)

1. <https://console.firebase.google.com> → tạo project.
2. **Project settings → Service accounts → Generate new private key** (JSON).
3. Base64 một dòng:

   ```bash
   base64 -w0 service-account.json > service-account.b64
   # macOS: base64 -i service-account.json | tr -d '\n' > service-account.b64
   ```

4. Render env:

   | Key | Value |
   |---|---|
   | `FCM_ENABLED` | `true` |
   | `FCM_CREDENTIALS_BASE64` | nội dung file `.b64` (secret) |

5. Nếu chưa làm mobile: để `FCM_ENABLED=false`. Notification center + WebSocket vẫn chạy,
   chỉ không có push.
6. Khi làm mobile: thêm Android app vào Firebase, tải `google-services.json` cho app;
   app lấy FCM token → `POST /api/v1/devices/token`.

---

## 5. Env đầy đủ trên Render

| Key | Bắt buộc | Ghi chú |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✅ | Neon, đã có từ phase 1 |
| `JWT_SECRET` | ✅ | đã có |
| `DB_POOL_SIZE` | ✅ | `5` |
| `JAVA_TOOL_OPTIONS` | ✅ | `-XX:MaxRAMPercentage=70 -Duser.timezone=UTC` |
| `STORAGE_TYPE` | ⚠️ | `supabase` hoặc để trống = local |
| `STORAGE_BASE_URL` | khi supabase | |
| `STORAGE_SERVICE_KEY` | khi supabase | secret |
| `STORAGE_BUCKET` | khi supabase | `remoteapp-uploads` |
| `STORAGE_PUBLIC_URL` | khi supabase | |
| `FCM_ENABLED` | ❌ | `false` mặc định |
| `FCM_CREDENTIALS_BASE64` | khi FCM bật | secret |
| `APP_TIMEZONE` | ❌ | mặc định `Asia/Ho_Chi_Minh` |
| `REMINDER_INTERVAL_MS` | ❌ | mặc định `300000` |
| `MAIL_*` / `MAIL_FROM` | ❌ | chỉ cần nếu muốn email reset thật |

---

## 6. Commit + push để deploy

Chỉ push khi `cd backend && ./mvnw clean verify` xanh.

```bash
git status
# commit theo Conventional Commits (ví dụ)
git add backend bruno docs render.yaml README.md AGENTS.md
git commit -m "feat(backend): hoan thien task/stats/chat/file cho phase 2"
git push origin main
```

CI sẽ:
1. `./mvnw -B test` (gồm Testcontainers).
2. `docker build`.
3. POST `RENDER_DEPLOY_HOOK_URL` (GitHub secret đã có từ phase 1).
4. Render build image + khởi động + Flyway migrate V3–V6 trên Neon.

Nếu `RENDER_DEPLOY_HOOK_URL` chưa có, job deploy chỉ ghi notice và bỏ qua — thêm secret rồi push lại.

---

## 7. Verify sau deploy

```bash
BASE=https://<service>.onrender.com

# 1. Health (cold start 30–60s)
curl -s "$BASE/actuator/health"           # {"status":"UP"}

# 2. Swagger/OpenAPI public
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/v3/api-docs"   # 200

# 3. Smoke read-only
cd bruno/collections/00-smoke
npx -y @usebruno/cli run --env-file ../../environments/render.yml
```

### 7.1 Flyway trên Neon

Neon → SQL editor:

```sql
select version, description, success from flyway_schema_history order by installed_rank;
-- phải thấy 1..6, tất cả success = true
```

### 7.2 Upload file thật (Supabase)

Cách nhanh nhất: chạy Bruno `01-api` trên Render (ghi dữ liệu demo) rồi xem kết quả:

```bash
cd bruno/collections/01-api
npx -y @usebruno/cli run \
  --env-file ../../environments/render.yml \
  --env-var inputEmailA="demo-a-$(date +%s)@example.com" \
  --env-var inputEmailB="demo-b-$(date +%s)@example.com" \
  --env-var inputEmailC="demo-c-$(date +%s)@example.com" \
  --env-var inputPasswordA=password-123 \
  --env-var inputPasswordB=password-123 \
  --env-var inputPasswordC=password-123 \
  --env-var inputNewPasswordA=password-123-v2 \
  --env-var inputUploadFile="$PWD/../../fixtures/sample.txt" \
  --env-var inputAvatarFile="$PWD/../../fixtures/sample.png"
```

Hoặc test tay: register → login → tạo project → `POST /tasks/{id}/attachments` (multipart).
Sau đó mở Supabase → Storage → bucket `remoteapp-uploads`, phải thấy object `{uuid}.txt`.

### 7.3 Notification/FCM

- `FCM_ENABLED=false`: gán task cho user → `GET /notifications` có bản ghi `TASK_ASSIGNED`;
  log Render ghi `[FCM disabled] Bỏ qua push`.
- `FCM_ENABLED=true`: cần device token thật; xem log `FcmPushSender` + Firebase Console.

---

## 8. Rollback

| Tình huống | Cách làm |
|---|---|
| Deploy mới lỗi | Render → Deploys → chọn bản trước → **Redeploy** (hoặc revert commit rồi push) |
| Lỗi do env Supabase | Xoá/đặt `STORAGE_TYPE` trống → Render restart về local |
| Lỗi do FCM | `FCM_ENABLED=false` → restart |
| Migration V3–V6 lỗi | Xem Render logs; V3–V6 chỉ `CREATE TABLE/INDEX`, sửa bằng migration mới `V7`, **không sửa file cũ** |
| Dữ liệu demo rác | Neon → `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` → redeploy (mất toàn bộ dữ liệu) |

---

## 9. Troubleshooting nhanh

| Lỗi | Nguyên nhân | Xử lý |
|---|---|---|
| Render healthcheck fail | thiếu `DB_*`/`JWT_SECRET`, `STORAGE_TYPE=supabase` thiếu key | xem Render logs; điền đủ env hoặc để `STORAGE_TYPE` trống |
| App chết với `Thiếu STORAGE_BASE_URL...` | bật supabase nhưng thiếu key | điền key hoặc tắt supabase |
| Upload 500 `STORAGE_ERROR` | sai bucket/key/URL | chạy cURL verify trong `02-dich-vu-ngoai.md` §2.3 |
| Upload 413 | file > 10MB | giảm file; avatar ≤ 5MB |
| Push không tới | `FCM_ENABLED=false` / thiếu credentials / token cũ | xem log `FcmPushSender` |
| Cold start | Render free ngủ 15 phút | client timeout ≥ 60s; UptimeRobot ping health |
| Neon chậm | autosuspend | giữ `DB_POOL_SIZE=5`, xem plan 01 |
| CI fail ở Testcontainers | Docker runner | GitHub-hosted runner có Docker; xem log `mvnw test` |

---

## 10. Checklist Definition of Done

- [ ] `cd backend && ./mvnw clean verify` xanh.
- [ ] Supabase bucket `remoteapp-uploads` public + 5 env `STORAGE_*` trên Render.
- [ ] (Tuỳ chọn) Firebase service account + `FCM_ENABLED`/`FCM_CREDENTIALS_BASE64`.
- [ ] Push `main` → CI xanh → Render deploy thành công.
- [ ] `/actuator/health` = `UP`.
- [ ] `flyway_schema_history` có version 6.
- [ ] Bruno `00-smoke` xanh trên `render.yml`.
- [ ] Upload file qua API → object xuất hiện trên Supabase → download lại được.
- [ ] Gán task → notification center có bản ghi.
- [ ] Không có secret trong repo (`git grep` không thấy key).
