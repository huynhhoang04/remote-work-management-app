---
name: "🧪 QA checklist (phase 3)"
about: "Theo dõi một lượt QA thủ công trên emulator / máy thật"
title: "[QA] Lượt kiểm <ngày> — <thiết bị>"
labels: "qa"
assignees: ""
---

<!-- Dùng khi chạy lại toàn bộ checklist. Mỗi mục lỗi → mở issue riêng "Báo lỗi" và dán link. -->

## Môi trường

| Trường | Giá trị |
|---|---|
| Thiết bị | |
| Kích thước / density | |
| Android | |
| App version / commit | |
| Backend | |
| Build | debug (Metro) / release APK |

## Auth
- [ ] Đăng ký tài khoản mới
- [ ] Đăng nhập / đăng xuất
- [ ] Quên mật khẩu → nhận mail
- [ ] Token hết hạn (15') → refresh im lặng, không văng login

## Hôm nay / Thống kê
- [ ] 4 card đếm đúng dữ liệu thật
- [ ] Năng suất cá nhân
- [ ] Dashboard dự án

## Dự án
- [ ] Tạo / sửa / xoá dự án
- [ ] Tạo link mời + join bằng mã 12 ký tự
- [ ] Đổi vai trò / xoá thành viên (Owner)
- [ ] Audit log chỉ Owner/Admin xem được

## Task
- [ ] Kanban 4 cột + kéo thả đổi trạng thái
- [ ] Đổi thứ tự (position)
- [ ] Tạo / sửa / xoá task; gán người; nhãn
- [ ] Checklist thêm/tick/xoá
- [ ] Bình luận; đính kèm file (upload + xoá)
- [ ] Lọc / tìm kiếm

## Lịch
- [ ] Xem tháng, chọn ngày
- [ ] Tạo / sửa / xoá sự kiện; liên kết task

## Chat
- [ ] Danh sách hội thoại GROUP/DIRECT
- [ ] Gửi tin realtime (2 tài khoản) + fallback REST
- [ ] Reply, read receipt, phân trang lịch sử

## Thông báo / Push
- [ ] Trung tâm, chưa đọc, đánh dấu đã đọc / tất cả
- [ ] Deep-link khi bấm thông báo
- [ ] Badge tab khi có tin mới
- [ ] Push FCM (chỉ khi `FCM_ENABLED=true`)

## Hồ sơ
- [ ] Sửa hồ sơ, upload avatar
- [ ] Bật/tắt nhận push

## Phi chức năng
- [ ] Render cold start: app loading/retry, không treo
- [ ] Mất mạng → StateView lỗi + retry
- [ ] Màn hình nhỏ 720x1280 không cắt chữ / tràn ngang
- [ ] Dark mode không sai màu

## Lỗi phát hiện

<!-- Liệt kê: - [ ] #<issue> — tóm tắt -->
