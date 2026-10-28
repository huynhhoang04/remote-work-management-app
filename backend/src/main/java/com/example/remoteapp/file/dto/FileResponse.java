package com.example.remoteapp.file.dto;

import com.example.remoteapp.file.FileMeta;
import java.util.UUID;

/**
 * DTO metadata file.
 *
 * @param id        id file
 * @param name      tên file gốc
 * @param mimeType  MIME type
 * @param size      kích thước (byte)
 * @param url       đường dẫn tải qua API
 * @param taskId    task đính kèm (null nếu không có)
 * @param messageId tin nhắn đính kèm (null nếu không có)
 * @reference docs/plan-backend/02-danh-sach-api.md §11
 */
public record FileResponse(UUID id, String name, String mimeType, long size, String url,
                           UUID taskId, UUID messageId) {

    /**
     * Ánh xạ entity sang DTO.
     *
     * @param file metadata file
     * @return DTO phản hồi
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.1 (mapping thủ công)
     */
    public static FileResponse from(FileMeta file) {
        return new FileResponse(file.getId(), file.getName(), file.getMimeType(), file.getSize(),
                "/api/v1/files/" + file.getId(), file.getTaskId(), file.getMessageId());
    }
}
