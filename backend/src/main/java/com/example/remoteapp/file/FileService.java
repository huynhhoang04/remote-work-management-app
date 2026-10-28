package com.example.remoteapp.file;

import com.example.remoteapp.common.AppException;
import com.example.remoteapp.common.ErrorCode;
import com.example.remoteapp.file.dto.FileResponse;
import com.example.remoteapp.project.ProjectPermissionService;
import com.example.remoteapp.project.ProjectRole;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Nghiệp vụ upload/download/xoá file đính kèm và avatar (plan 03 §H).
 *
 * <p>File gắn dự án/task/chat thì kiểm quyền qua {@link ProjectPermissionService};
 * file không gắn dự án (avatar) chỉ cần đã đăng nhập.</p>
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 */
@Service
public class FileService {

    private static final long MAX_ATTACHMENT_BYTES = 10L * 1024 * 1024;
    private static final long MAX_AVATAR_BYTES = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "application/zip");

    private static final Set<String> IMAGE_MIME = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final ProjectPermissionService permissionService;

    /**
     * Khởi tạo service (constructor injection tường minh).
     *
     * @param fileRepository      repository metadata file
     * @param storageService      cổng lưu trữ thật (local/Supabase)
     * @param permissionService   kiểm tra quyền theo dự án
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1
     */
    public FileService(FileRepository fileRepository, StorageService storageService,
                       ProjectPermissionService permissionService) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.permissionService = permissionService;
    }

    /**
     * Upload file đính kèm (task/chat hoặc file tự do trong dự án).
     *
     * @param actorId   người upload
     * @param file      file multipart
     * @param projectId dự án liên quan (null nếu không gắn dự án)
     * @param taskId    task đính kèm (null nếu không)
     * @param messageId tin nhắn đính kèm (null nếu không)
     * @return metadata file
     * @throws AppException FILE_TYPE_NOT_ALLOWED / FILE_TOO_LARGE / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Transactional
    public FileResponse storeAttachment(UUID actorId, MultipartFile file, UUID projectId,
                                        UUID taskId, UUID messageId) {
        if (projectId != null) {
            permissionService.requireRole(projectId, actorId, ProjectRole.MEMBER);
        }
        validate(file, ALLOWED_MIME, MAX_ATTACHMENT_BYTES);
        FileMeta meta = persist(actorId, file, projectId, taskId, messageId);
        return FileResponse.from(meta);
    }

    /**
     * Upload avatar (chỉ ảnh, ≤ 5MB) — trả URL tương đối để lưu vào hồ sơ.
     *
     * @param actorId người dùng
     * @param file    file ảnh
     * @return URL tải avatar qua API
     * @throws AppException FILE_TYPE_NOT_ALLOWED / FILE_TOO_LARGE
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §A4
     */
    @Transactional
    public String storeAvatar(UUID actorId, MultipartFile file) {
        validate(file, IMAGE_MIME, MAX_AVATAR_BYTES);
        FileMeta meta = persist(actorId, file, null, null, null);
        return "/api/v1/files/" + meta.getId();
    }

    /**
     * Mở file để tải về (kiểm quyền theo dự án nếu file gắn dự án).
     *
     * @param actorId người tải
     * @param fileId  id file
     * @return metadata + luồng nội dung
     * @throws AppException FILE_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Transactional(readOnly = true)
    public FileDownload open(UUID actorId, UUID fileId) {
        FileMeta meta = findFile(fileId);
        if (meta.getProjectId() != null) {
            permissionService.requireMember(meta.getProjectId(), actorId);
        }
        return new FileDownload(meta.getName(), meta.getMimeType(), meta.getSize(),
                storageService.open(meta.getStorageKey()));
    }

    /**
     * Xoá file — người upload hoặc OWNER/ADMIN dự án.
     *
     * @param actorId người yêu cầu
     * @param fileId  id file
     * @throws AppException FILE_NOT_FOUND / PROJECT_FORBIDDEN
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Transactional
    public void delete(UUID actorId, UUID fileId) {
        FileMeta meta = findFile(fileId);
        if (!meta.getOwnerId().equals(actorId)) {
            if (meta.getProjectId() == null) {
                throw new AppException(ErrorCode.PROJECT_FORBIDDEN,
                        "Bạn không có quyền xoá file này");
            }
            permissionService.requireRole(meta.getProjectId(), actorId, ProjectRole.ADMIN);
        }
        storageService.delete(meta.getStorageKey());
        fileRepository.delete(meta);
    }

    /**
     * Xoá file đính kèm của một task (kiểm tra file đúng task).
     *
     * @param actorId người yêu cầu
     * @param taskId  task chứa file
     * @param fileId  id file
     * @throws AppException FILE_NOT_FOUND nếu file không thuộc task
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Transactional
    public void deleteAttachment(UUID actorId, UUID taskId, UUID fileId) {
        FileMeta meta = findFile(fileId);
        if (!taskId.equals(meta.getTaskId())) {
            throw new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file đính kèm");
        }
        delete(actorId, fileId);
    }

    /**
     * Liệt kê file đính kèm của task (dùng khi dựng TaskResponse).
     *
     * @param taskId id task
     * @return danh sách metadata file
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    @Transactional(readOnly = true)
    public List<FileResponse> listByTask(UUID taskId) {
        return fileRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(FileResponse::from)
                .toList();
    }

    /**
     * Kiểm tra MIME + kích thước file.
     *
     * @param file    file multipart
     * @param allowed tập MIME được phép
     * @param maxSize kích thước tối đa (byte)
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    private void validate(MultipartFile file, Set<String> allowed, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "File rỗng");
        }
        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE,
                    "File vượt quá giới hạn " + (maxSize / (1024 * 1024)) + "MB");
        }
        String mime = file.getContentType() == null
                ? "application/octet-stream"
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!allowed.contains(mime)) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "Loại file không được phép: " + mime);
        }
    }

    /**
     * Lưu nội dung vào storage + metadata vào DB (xoá object nếu DB lỗi).
     *
     * @param actorId   người upload
     * @param file      file multipart
     * @param projectId dự án (có thể null)
     * @param taskId    task (có thể null)
     * @param messageId tin nhắn (có thể null)
     * @return metadata đã lưu
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    private FileMeta persist(UUID actorId, MultipartFile file, UUID projectId,
                             UUID taskId, UUID messageId) {
        String storageKey = UUID.randomUUID() + extensionOf(file);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Không đọc được file");
        }
        storageService.store(storageKey, content, file.getContentType());

        FileMeta meta = new FileMeta();
        meta.setOwnerId(actorId);
        meta.setProjectId(projectId);
        meta.setTaskId(taskId);
        meta.setMessageId(messageId);
        meta.setName(originalName(file));
        meta.setMimeType(file.getContentType());
        meta.setSize(file.getSize());
        meta.setStorageKey(storageKey);
        try {
            return fileRepository.save(meta);
        } catch (RuntimeException ex) {
            storageService.delete(storageKey);
            throw ex;
        }
    }

    /**
     * Tìm metadata file hoặc ném lỗi thống nhất.
     *
     * @param fileId id file
     * @return metadata
     * @throws AppException FILE_NOT_FOUND
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    private FileMeta findFile(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND, "Không tìm thấy file"));
    }

    /**
     * Lấy tên file gốc đã làm sạch (bỏ đường dẫn, giới hạn 255 ký tự).
     *
     * @param file file multipart
     * @return tên file an toàn
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    private String originalName(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "file";
        }
        String cleaned = name.replace('\\', '/');
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).trim();
        return cleaned.length() > 255 ? cleaned.substring(cleaned.length() - 255) : cleaned;
    }

    /**
     * Sinh phần mở rộng file từ tên gốc (fallback theo MIME).
     *
     * @param file file multipart
     * @return chuỗi bắt đầu bằng "." hoặc rỗng
     * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
     */
    private String extensionOf(MultipartFile file) {
        String name = originalName(file);
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            if (ext.matches("[a-z0-9]{1,10}")) {
                return "." + ext;
            }
        }
        String mime = file.getContentType();
        if (mime == null) {
            return "";
        }
        return switch (mime.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            case "application/zip" -> ".zip";
            default -> "";
        };
    }

    /**
     * Kết quả tải file (metadata + luồng nội dung).
     *
     * @param name      tên file
     * @param mimeType  MIME type
     * @param size      kích thước
     * @param stream    luồng nội dung
     * @reference docs/plan-backend/02-danh-sach-api.md §11
     */
    public record FileDownload(String name, String mimeType, long size, InputStream stream) {
    }
}
