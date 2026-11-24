package com.example.remoteapp.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit test cho {@link SupabaseStorageService} — chuẩn hoá env dán kèm khoảng trắng/dấu nháy/`/`,
 * chọn header xác thực theo loại key, và map lỗi upstream đúng mã nghiệp vụ.
 *
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §2 và §7
 */
class SupabaseStorageServiceTest {

    private static final String BASE_URL = "https://demo-ref.supabase.co";
    private static final String JWT_KEY = "eyJhbGciOiJIUzI1NiJ9.fake.payload.signature";
    private static final String NEW_KEY = "sb_secret_1234567890abcdef";

    /** Dựng service + mock server, trả về bộ ba để assert. */
    private record Fixture(SupabaseStorageService service, MockRestServiceServer server) {
    }

    private Fixture fixture(String baseUrl, String key) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new SupabaseStorageService(baseUrl, "remoteapp-uploads", key, builder), server);
    }

    /** @reference docs/plan-deploy/02-dich-vu-ngoai.md §2.2 */
    @Test
    void store_normalizesBaseUrlAndKeyDirtiedByDashboard() {
        Fixture fixture = fixture("  \"https://demo-ref.supabase.co/\"\n", JWT_KEY);

        fixture.server().expect(once(),
                        requestTo("https://demo-ref.supabase.co/storage/v1/object/remoteapp-uploads/abc.txt"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", JWT_KEY))
                .andExpect(header("Authorization", "Bearer " + JWT_KEY))
                .andExpect(content().bytes("ping".getBytes(StandardCharsets.UTF_8)))
                .andRespond(withSuccess());

        fixture.service().store("abc.txt", "ping".getBytes(StandardCharsets.UTF_8), "text/plain");
        fixture.server().verify();
    }

    /** @reference docs/plan-deploy/02-dich-vu-ngoai.md §2.5 */
    @Test
    void store_baseUrlPastedWithStorageSuffixStillTargetsProjectUrl() {
        Fixture fixture = fixture("https://demo-ref.supabase.co/storage/v1/", JWT_KEY);

        fixture.server().expect(once(),
                        requestTo("https://demo-ref.supabase.co/storage/v1/object/remoteapp-uploads/abc.txt"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        fixture.service().store("abc.txt", "ping".getBytes(StandardCharsets.UTF_8), "text/plain");
        fixture.server().verify();
    }

    /** Key mới {@code sb_secret_*} không phải JWT nên chỉ gửi apikey, không gửi Bearer. */
    @Test
    void store_newApiKeySendsOnlyApikeyHeader() {
        Fixture fixture = fixture(BASE_URL, NEW_KEY);

        fixture.server().expect(once(),
                        requestTo(BASE_URL + "/storage/v1/object/remoteapp-uploads/abc.txt"))
                .andExpect(header("apikey", NEW_KEY))
                .andExpect(request -> assertThat(request.getHeaders().getFirst("Authorization")).isNull())
                .andRespond(withSuccess());

        fixture.service().store("abc.txt", "x".getBytes(StandardCharsets.UTF_8), "text/plain");
        fixture.server().verify();
    }

    /** MIME hỏng/thiếu không làm request vỡ — fallback octet-stream. */
    @Test
    void store_fallsBackToOctetStreamForBlankContentType() {
        Fixture fixture = fixture(BASE_URL, JWT_KEY);

        fixture.server().expect(once(), requestTo(BASE_URL + "/storage/v1/object/remoteapp-uploads/abc.bin"))
                .andExpect(header("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andRespond(withSuccess());

        fixture.service().store("abc.bin", "x".getBytes(StandardCharsets.UTF_8), null);
        fixture.server().verify();
    }

    /** Upstream 401 phải thành STORAGE_ERROR; message trả client không lộ tên biến env. */
    @Test
    void store_mapsUpstreamFailureToStorageError() {
        Fixture fixture = fixture(BASE_URL, JWT_KEY);

        fixture.server().expect(once(), requestTo(BASE_URL + "/storage/v1/object/remoteapp-uploads/abc.txt"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Invalid JWT\"}"));

        assertThatThrownBy(() -> fixture.service().store("abc.txt",
                        "x".getBytes(StandardCharsets.UTF_8), "text/plain"))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STORAGE_ERROR);
                    assertThat(ex.getMessage()).doesNotContain("STORAGE_");
                });
        fixture.server().verify();
    }

    /** Bucket/URL sai (404) vẫn trả STORAGE_ERROR thân thiện. */
    @Test
    void store_mapsNotFoundToStorageError() {
        Fixture fixture = fixture(BASE_URL, JWT_KEY);

        fixture.server().expect(once(),
                        requestTo(BASE_URL + "/storage/v1/object/remoteapp-uploads/abc.txt"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> fixture.service().store("abc.txt",
                        "x".getBytes(StandardCharsets.UTF_8), "text/plain"))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STORAGE_ERROR);
                    assertThat(ex.getMessage()).doesNotContain("STORAGE_");
                });
        fixture.server().verify();
    }

    /** Tải file thiếu object → FILE_NOT_FOUND (không phải 500). */
    @Test
    void open_mapsNotFoundToFileNotFound() {
        Fixture fixture = fixture(BASE_URL, JWT_KEY);

        fixture.server().expect(once(),
                        requestTo(BASE_URL + "/storage/v1/object/remoteapp-uploads/missing.txt"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> fixture.service().open("missing.txt"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FILE_NOT_FOUND));
        fixture.server().verify();
    }

    /** Thiếu key → fail-fast ngay khi khởi tạo bean. */
    @Test
    void constructor_rejectsBlankKey() {
        assertThatThrownBy(() -> new SupabaseStorageService(BASE_URL, "bucket", "   \n"))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STORAGE_ERROR));
    }

    /** Base URL rỗng → fail-fast. */
    @Test
    void constructor_rejectsBlankBaseUrl() {
        assertThatThrownBy(() -> new SupabaseStorageService("", "bucket", JWT_KEY))
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STORAGE_ERROR));
    }
}
