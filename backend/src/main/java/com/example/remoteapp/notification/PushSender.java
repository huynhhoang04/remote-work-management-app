package com.example.remoteapp.notification;

import java.util.List;
import java.util.Map;

/**
 * Cổng gửi push notification — FCM khi có credentials, no-op khi chưa cấu hình.
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
 */
public interface PushSender {

    /**
     * Gửi push tới danh sách FCM token.
     *
     * @param tokens danh sách token thiết bị
     * @param title  tiêu đề
     * @param body   nội dung
     * @param data   dữ liệu kèm theo (refType, refId...)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §E2
     */
    void push(List<String> tokens, String title, String body, Map<String, String> data);
}
