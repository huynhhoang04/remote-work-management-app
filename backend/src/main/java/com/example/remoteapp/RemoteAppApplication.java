package com.example.remoteapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm vào của backend — modular monolith duy nhất (plan 01 §1).
 *
 * <p>Cấu trúc package theo feature: auth, user, project, task, schedule, stats,
 * chat, notification, audit, file (plan 01 §3). Các module chưa có ở bước hiện tại
 * sẽ được thêm theo thứ tự plan 03 mục I.</p>
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §1, §3
 */
@SpringBootApplication
@EnableScheduling
public class RemoteAppApplication {

    /**
     * Khởi chạy ứng dụng Spring Boot.
     *
     * @param args tham số dòng lệnh
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §6.2
     */
    public static void main(String[] args) {
        SpringApplication.run(RemoteAppApplication.class, args);
    }
}
