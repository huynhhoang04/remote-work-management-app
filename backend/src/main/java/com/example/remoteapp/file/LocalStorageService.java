package com.example.remoteapp.file;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Lưu file trên ổ đĩa local — dùng cho dev/test (Render free không có persistent disk,
 * production dùng {@link SupabaseStorageService}).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 * @reference docs/plan-deploy/02-dich-vu-ngoai.md §1
 */
public class LocalStorageService implements StorageService {

    private final Path root;

    /**
     * Khởi tạo storage local (constructor injection tường minh).
     *
     * @param uploadDir thư mục lưu file (app.upload.dir)
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public LocalStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Không tạo được thư mục upload");
        }
    }

    /**
     * Ghi file xuống đĩa.
     *
     * @param key         khoá lưu trữ
     * @param content     nội dung file
     * @param contentType MIME type (không dùng khi lưu local)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Không lưu được file");
        }
    }

    /**
     * Mở luồng đọc file trên đĩa.
     *
     * @param key khoá lưu trữ
     * @return luồng nội dung file
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Override
    public InputStream open(String key) {
        Path target = resolve(key);
        try {
            return Files.newInputStream(target);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file");
        }
    }

    /**
     * Xoá file trên đĩa (bỏ qua nếu không tồn tại).
     *
     * @param key khoá lưu trữ
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Không xoá được file");
        }
    }

    /**
     * Ánh xạ khoá lưu trữ sang đường dẫn tuyệt đối trong thư mục upload.
     *
     * @param key khoá lưu trữ
     * @return đường dẫn an toàn (chặn path traversal)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Khoá lưu trữ không hợp lệ");
        }
        return target;
    }
}
