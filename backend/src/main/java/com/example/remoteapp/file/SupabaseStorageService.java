package com.example.remoteapp.file;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Lưu file trên Supabase Storage qua REST API (bucket public cho demo).
 *
 * <p>Không thêm SDK: chỉ dùng {@link RestClient} của Spring 6.1. Muốn đổi sang MinIO/S3
 * thì viết implementation khác của {@link StorageService} và đổi {@code app.storage.type}.</p>
 *
 * <p><b>Chuẩn hoá cấu hình:</b> giá trị env do dashboard/CI dán vào thường dính khoảng trắng,
 * xuống dòng, dấu {@code "} hoặc dấu {@code /} cuối — trước đây chúng bị đẩy thẳng vào URL/header
 * khiến Supabase trả 400/401 mà client chỉ thấy "Không upload được file lên storage".
 * Service tự trim và bỏ dấu {@code /} cuối. Key legacy (service_role JWT, bắt đầu {@code eyJ})
 * gửi cả {@code apikey} + {@code Authorization: Bearer}; key mới ({@code sb_secret_...}) chỉ gửi
 * {@code apikey} vì Bearer với chuỗi không phải JWT sẽ bị từ chối.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
 */
@Slf4j
public class SupabaseStorageService implements StorageService {

    private static final String STORAGE_PATH_PREFIX = "/storage/v1/object";
    private static final int LOGGED_BODY_LIMIT = 500;

    private final RestClient restClient;
    private final String bucket;
    private final String serviceKey;
    private final boolean serviceKeyIsJwt;

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
        this(baseUrl, bucket, serviceKey, RestClient.builder());
    }

    /**
     * Constructor dành cho test: cho phép chèn {@link RestClient.Builder} đã bind
     * {@code MockRestServiceServer}.
     *
     * @param baseUrl    project URL của Supabase
     * @param bucket     tên bucket
     * @param serviceKey service_role key
     * @param builder    builder RestClient (test bind mock server)
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    SupabaseStorageService(String baseUrl, String bucket, String serviceKey,
                            RestClient.Builder builder) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        String normalizedKey = normalizeSecret(serviceKey);
        String normalizedBucket = normalizeSecret(bucket);
        if (normalizedBaseUrl == null || normalizedKey == null || normalizedBucket == null) {
            throw new AppException(ErrorCode.STORAGE_ERROR,
                    "Thiếu STORAGE_BASE_URL hoặc STORAGE_SERVICE_KEY");
        }
        this.restClient = builder.baseUrl(normalizedBaseUrl).build();
        this.bucket = normalizedBucket;
        this.serviceKey = normalizedKey;
        this.serviceKeyIsJwt = normalizedKey.startsWith("eyJ");
        log.info("Supabase Storage: baseUrl={}, bucket={}, keyType={}", normalizedBaseUrl,
                normalizedBucket, serviceKeyIsJwt ? "service_role-JWT" : "new-api-key");
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
                    .uri(STORAGE_PATH_PREFIX + "/{bucket}/{key}", bucket, key)
                    .headers(headers -> {
                        applyAuth(headers);
                        headers.setContentType(resolveMediaType(contentType));
                        headers.set("x-upsert", "true");
                    })
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            logUpstreamFailure("upload", key, ex);
            throw toStorageException(ex, "Không upload được file lên storage");
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
                    .uri(STORAGE_PATH_PREFIX + "/{bucket}/{key}", bucket, key)
                    .headers(this::applyAuth)
                    .retrieve()
                    .body(byte[].class);
            if (body == null) {
                throw new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file");
            }
            return new ByteArrayInputStream(body);
        } catch (RestClientResponseException ex) {
            logUpstreamFailure("download", key, ex);
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file");
            }
            throw toStorageException(ex, "Không tải được file từ storage");
        } catch (RestClientException ex) {
            logUpstreamFailure("download", key, ex);
            throw toStorageException(ex, "Không tải được file từ storage");
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
                    .uri(STORAGE_PATH_PREFIX + "/{bucket}/{key}", bucket, key)
                    .headers(this::applyAuth)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // File có thể đã bị xoá trước đó — không chặn thao tác xoá metadata
            log.debug("Supabase Storage: xoá object {} không thành công (bỏ qua): {}", key,
                    describe(ex));
        }
    }

    /**
     * Gắn header xác thực theo loại key: key JWT legacy nhận cả Bearer, key mới chỉ nhận apikey.
     *
     * @param headers header của request
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2.5
     */
    private void applyAuth(HttpHeaders headers) {
        headers.set("apikey", serviceKey);
        if (serviceKeyIsJwt) {
            headers.setBearerAuth(serviceKey);
        }
    }

    /**
     * Ghi log lỗi upstream (HTTP status + body ngắn) để Render log đủ dữ liệu chẩn đoán
     * mà không lộ key. Trước đây exception bị nuốt hoàn toàn nên sự cố Supabase
     * "tồn tại rất lâu" mà không ai thấy nguyên nhân.
     *
     * @param operation thao tác (upload/download)
     * @param key       khoá lưu trữ
     * @param ex        exception upstream
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §7
     */
    private void logUpstreamFailure(String operation, String key, RestClientException ex) {
        log.error("Supabase Storage: {} object {} thất bại — {}", operation, key, describe(ex));
    }

    /**
     * Đổi lỗi upstream thành {@link AppException} thân thiện với người dùng, đồng thời
     * ghi gợi ý cấu hình (theo HTTP status) vào log để người vận hành biết sửa biến nào.
     * Không đưa tên biến môi trường vào message trả client.
     *
     * @param ex      exception upstream
     * @param message message nghiệp vụ cơ bản
     * @return exception nghiệp vụ STORAGE_ERROR
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §7
     */
    private AppException toStorageException(RestClientException ex, String message) {
        if (ex instanceof RestClientResponseException responseException) {
            HttpStatus status = HttpStatus.resolve(responseException.getStatusCode().value());
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                log.error("Gợi ý: kiểm tra STORAGE_SERVICE_KEY (key sai/hết hiệu lực hoặc dùng anon key)");
            } else if (status == HttpStatus.NOT_FOUND) {
                log.error("Gợi ý: kiểm tra STORAGE_BASE_URL và STORAGE_BUCKET (bucket chưa tạo?)");
            }
        }
        return new AppException(ErrorCode.STORAGE_ERROR,
                message + ", vui lòng thử lại sau");
    }

    /**
     * Mô tả ngắn exception upstream (status + body, cắt bớt để không phình log).
     *
     * @param ex exception upstream
     * @return chuỗi chẩn đoán không chứa key
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §7
     */
    private String describe(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString(StandardCharsets.UTF_8);
            if (body.length() > LOGGED_BODY_LIMIT) {
                body = body.substring(0, LOGGED_BODY_LIMIT) + "…";
            }
            return "HTTP " + responseException.getStatusCode().value() + " " + body;
        }
        return ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }

    /**
     * Chuẩn hoá URL project: bỏ khoảng trắng, dấu nháy dán kèm, dấu {@code /} cuối
     * và đuôi {@code /storage/v1} (nếu user dán URL Storage API thay vì project URL).
     *
     * @param raw giá trị env gốc
     * @return URL đã chuẩn hoá hoặc {@code null} nếu rỗng
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2.2
     */
    private static String normalizeBaseUrl(String raw) {
        String value = normalizeSecret(raw);
        if (value == null) {
            return null;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/storage/v1")) {
            value = value.substring(0, value.length() - "/storage/v1".length());
        }
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("URL không có scheme/host");
            }
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.STORAGE_ERROR,
                    "STORAGE_BASE_URL không hợp lệ (cần https://<ref>.supabase.co)");
        }
        return value;
    }

    /**
     * Chuẩn hoá secret: trim, bỏ dấu nháy bao ngoài, bỏ tiền tố {@code Bearer }.
     *
     * @param raw giá trị env gốc
     * @return giá trị đã chuẩn hoá hoặc {@code null} nếu rỗng
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2.2
     */
    private static String normalizeSecret(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.replace("\r", "").replace("\n", "").trim();
        while (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            value = value.substring(7).trim();
        }
        return value.isBlank() ? null : value;
    }

    /**
     * Đổi chuỗi MIME thành {@link MediaType}, fallback octet-stream nếu thiếu/hỏng.
     *
     * @param contentType MIME từ client (có thể null)
     * @return media type an toàn để gửi
     * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2
     */
    private static MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
