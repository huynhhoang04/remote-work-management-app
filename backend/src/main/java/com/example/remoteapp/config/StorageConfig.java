package com.example.remoteapp.config;

import com.example.remoteapp.file.LocalStorageService;
import com.example.remoteapp.file.StorageService;
import com.example.remoteapp.file.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chọn implementation lưu trữ theo {@code app.storage.type}.
 *
 * <p>Chọn bằng code thay vì {@code @ConditionalOnProperty} để biến môi trường rỗng
 * (Render có thể set chuỗi rỗng) vẫn rơi về local thay vì thiếu bean.</p>
 *
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §8 (fallback matrix)
 */
@Configuration
public class StorageConfig {

    /**
     * Tạo bean StorageService: {@code supabase} khi cấu hình đầy đủ, còn lại dùng local.
     *
     * @param type      loại storage (local/supabase)
     * @param uploadDir thư mục local
     * @param baseUrl   Supabase project URL
     * @param bucket    bucket
     * @param serviceKey service_role key
     * @return implementation tương ứng
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    @Bean
    public StorageService storageService(@Value("${app.storage.type:local}") String type,
                                         @Value("${app.upload.dir:uploads}") String uploadDir,
                                         @Value("${app.storage.base-url:}") String baseUrl,
                                         @Value("${app.storage.bucket:remoteapp-uploads}") String bucket,
                                         @Value("${app.storage.service-key:}") String serviceKey) {
        if ("supabase".equalsIgnoreCase(type.trim())) {
            return new SupabaseStorageService(baseUrl, bucket, serviceKey);
        }
        return new LocalStorageService(uploadDir);
    }
}
