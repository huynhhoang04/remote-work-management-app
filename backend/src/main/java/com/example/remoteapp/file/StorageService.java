package com.example.remoteapp.file;

import java.io.InputStream;

/**
 * Cổng lưu trữ file — tách khỏi nơi lưu thật để đổi đĩa local sang object storage
 * (Supabase/MinIO/S3) mà không đổi API (plan 03 §H).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §1
 */
public interface StorageService {

    /**
     * Lưu file theo khoá do hệ thống sinh.
     *
     * @param key         khoá lưu trữ (uuid.ext)
     * @param content     nội dung file
     * @param contentType MIME type
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    void store(String key, byte[] content, String contentType);

    /**
     * Mở luồng đọc file.
     *
     * @param key khoá lưu trữ
     * @return luồng nội dung file
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    InputStream open(String key);

    /**
     * Xoá file khỏi nơi lưu trữ (bỏ qua nếu không tồn tại).
     *
     * @param key khoá lưu trữ
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    void delete(String key);
}
