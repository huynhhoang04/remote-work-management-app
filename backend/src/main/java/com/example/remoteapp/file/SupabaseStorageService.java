package com.example.remoteapp.file;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Lưu file trên Supabase Storage qua REST API (bucket public cho demo).
 *
 * <p>Không thêm SDK: chỉ dùng {@link RestClient} của Spring 6.1. Muốn đổi sang MinIO/S3
 * thì viết implementation khác của {@link StorageService} và đổi {@code app.storage.type}.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
 */
public class SupabaseStorageService implements StorageService {

    private final RestClient restClient;
    private final String bucket;
    private final String serviceKey;

    /**
     * Khởi tạo storage Supabase (constructor injection tường minh).
     *
     * @param baseUrl    project URL của Supabase
     * @param bucket     tên bucket
     * @param serviceKey service_role key (không commit, không đưa xuống mobile)
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    public SupabaseStorageService(@Value("${app.storage.base-url}") String baseUrl,
                                  @Value("${app.storage.bucket}") String bucket,
                                  @Value("${app.storage.service-key}") String serviceKey) {
        if (baseUrl == null || baseUrl.isBlank() || serviceKey == null || serviceKey.isBlank()) {
            throw new AppException(ErrorCode.STORAGE_ERROR,
                    "Thiếu STORAGE_BASE_URL hoặc STORAGE_SERVICE_KEY");
        }
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.bucket = bucket;
        this.serviceKey = serviceKey;
    }

    /**
     * Upload file lên bucket qua Storage REST API.
     *
     * @param key         khoá lưu trữ
     * @param content     nội dung file
     * @param contentType MIME type
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    @Override
    public void store(String key, byte[] content, String contentType) {
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .headers(headers -> {
                        headers.set("apikey", serviceKey);
                        headers.setBearerAuth(serviceKey);
                        headers.setContentType(MediaType.parseMediaType(contentType));
                        headers.set("x-upsert", "true");
                    })
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Không upload được file lên storage");
        }
    }

    /**
     * Tải nội dung file từ bucket.
     *
     * @param key khoá lưu trữ
     * @return luồng nội dung file
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    @Override
    public InputStream open(String key) {
        try {
            byte[] body = restClient.get()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .headers(headers -> {
                        headers.set("apikey", serviceKey);
                        headers.setBearerAuth(serviceKey);
                    })
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file");
            }
            return new ByteArrayInputStream(body);
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file");
        }
    }

    /**
     * Xoá file khỏi bucket.
     *
     * @param key khoá lưu trữ
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    @Override
    public void delete(String key) {
        try {
            restClient.delete()
                    .uri("/storage/v1/object/{bucket}/{key}", bucket, key)
                    .headers(headers -> {
                        headers.set("apikey", serviceKey);
                        headers.setBearerAuth(serviceKey);
                    })
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // File có thể đã bị xoá trước đó — không chặn thao tác xoá metadata
        }
    }
}
