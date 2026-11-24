---
name: "🐞 Báo lỗi (bug/UI)"
about: "Ghi nhận lỗi phát hiện khi QA trên emulator hoặc máy thật"
title: "[MOB-XXX] "
labels: "bug, mobile"
assignees: ""
---

<!--
Quy ước (xem docs/plan-app/02-phase-2.md §9.2):
- Mỗi issue = 1 lỗi trên 1 màn hình/route cụ thể.
- Mức độ: P0 chặn dùng (crash/mất dữ liệu) · P1 sai chức năng chính · P2 sai giao diện/UX · P3 góp ý nhỏ.
- Trạng thái đi theo: Mới → Đang sửa → Đã sửa → Đóng.
-->

## Mô tả

<!-- Một câu tóm tắt lỗi. -->

## Mức độ & loại

- [ ] P0 — chặn dùng (crash / mất dữ liệu / không đăng nhập được)
- [ ] P1 — sai chức năng chính
- [ ] P2 — sai giao diện / UX
- [ ] P3 — góp ý nhỏ

Loại: `bug` · `ui` · `ux` · `perf` · `backend` · `docs`

## Môi trường

| Trường | Giá trị |
|---|---|
| Thiết bị | <!-- Emulator Small_Phone / máy thật + model --> |
| Kích thước / density | <!-- 720x1280 @320dpi --> |
| Android | <!-- API 35 / Android 14 --> |
| App version | <!-- 1.0.0 --> |
| Commit | <!-- sha ngắn --> |
| Backend | <!-- Render: https://remoteapp-backend.onrender.com / local :8080 --> |
| Build | <!-- debug (Metro) / release APK --> |

## Màn hình / route

<!-- mobile/src/app/(app)/... -->

## Bước tái hiện

1.
2.
3.

## Kỳ vọng

<!-- Điều đúng ra phải xảy ra. -->

## Thực tế

<!-- Điều đang xảy ra (dán log / ảnh chụp nếu có). -->

## Ảnh / log

<!-- Kéo ảnh vào đây, hoặc dán logcat / Metro log. -->

## Hướng fix (nếu đoán được)

<!-- File/route nghi ngờ, nguyên nhân. Điền khi đóng issue: commit + file:line. -->
