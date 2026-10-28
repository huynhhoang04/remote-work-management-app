package com.example.remoteapp.file;

import com.example.remoteapp.common.SecurityUtils;
import com.example.remoteapp.file.dto.FileResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller upload/download/xoá file (plan 02 §11).
 *
 * <p>File đính kèm task dùng thêm endpoint trong {@code TaskController}; endpoint
 * {@code POST /files} dành cho file tự do trong dự án (kèm {@code projectId} tuỳ chọn).</p>
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §11
 */
@RestController
@RequestMapping("/api/v1")
public class FileController {

    private final FileService fileService;

    /**
     * Khởi tạo controller (constructor injection tường minh).
     *
     * @param fileService service file
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Upload file (multipart) — có thể gắn dự án qua query param.
     *
     * @param file      file cần upload
     * @param projectId dự án liên quan (tuỳ chọn)
     * @return 201 + metadata file
     * @reference docs/plan-backend/02-danh-sach-api.md §11
     */
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileResponse> upload(@RequestPart("file") MultipartFile file,
                                               @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.status(201).body(fileService.storeAttachment(
                SecurityUtils.currentUserId(), file, projectId, null, null));
    }

    /**
     * Tải file — kiểm quyền theo dự án nếu file gắn dự án.
     *
     * @param id id file
     * @return luồng nội dung kèm header tải về
     * @reference docs/plan-backend/02-danh-sach-api.md §11
     */
    @GetMapping("/files/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id) {
        FileService.FileDownload file = fileService.open(SecurityUtils.currentUserId(), id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.name(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(file.stream()));
    }

    /**
     * Xoá file (người upload hoặc OWNER/ADMIN dự án).
     *
     * @param id id file
     * @return 204
     * @reference docs/plan-backend/02-danh-sach-api.md §11
     */
    @DeleteMapping("/files/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fileService.delete(SecurityUtils.currentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
