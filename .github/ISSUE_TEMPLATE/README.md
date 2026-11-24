# Cấu hình issue template cho repo

Template lỗi/QA đặt trong `.github/ISSUE_TEMPLATE/`.
Khi mở issue mới trên GitHub, chọn một trong ba mẫu:

| Template | Dùng cho |
|---|---|
| `bug_report.md` | Một lỗi cụ thể trên một màn hình/route — có đủ trường môi trường, bước tái hiện |
| `feature_request.md` | Góp ý / tinh chỉnh nhỏ (đã kiểm tra không thuộc phạm vi bị cắt ở `docs/04` §1.2) |
| `qa_checklist.md` | Một lượt QA thủ công toàn bộ app; lỗi con mở issue riêng và link lại |

Quy ước ID, mức độ, trạng thái và checklist giống `docs/plan-app/03-issues.md` (nếu file đó
tồn tại) hoặc mục §9.2 của `docs/plan-app/02-phase-2.md`.

Nhãn gợi ý (tạo trong Settings → Labels nếu repo chưa có): `bug`, `ui`, `ux`, `perf`, `backend`,
`mobile`, `qa`, `enhancement`, `P0`, `P1`, `P2`, `P3`.
