package com.example.remoteapp.notification;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Push sender mặc định khi chưa cấu hình Firebase — chỉ log, không gửi.
 *
 * <p>Nhờ vậy backend chạy được trên môi trường không có service account; trung tâm
 * thông báo vẫn hoạt động bình thường. Bean được tạo bởi {@code PushConfig}.</p>
 *
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §3
 */
@Slf4j
public class NoopPushSender implements PushSender {

    /**
     * Ghi log số token sẽ nhận push (không gửi thật).
     *
     * @param tokens danh sách token
     * @param title  tiêu đề
     * @param body   nội dung
     * @param data   dữ liệu kèm theo
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §3
     */
    @Override
    public void push(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            return;
        }
        log.info("[FCM disabled] Bỏ qua push '{}' tới {} thiết bị (data={})", title, tokens.size(), data);
    }
}
