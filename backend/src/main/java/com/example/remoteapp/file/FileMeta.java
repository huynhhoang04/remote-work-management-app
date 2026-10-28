package com.example.remoteapp.file;

import com.example.remoteapp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Metadata file đính kèm (nội dung nằm ở {@link StorageService}).
 *
 * @reference docs/plan-backend/03-chuc-nang-dac-ta-va-implementation.md §H
 * @reference docs/04-thiet-ke-backend.md §5.2
 */
@Entity
@Table(name = "files")
@Getter
@Setter
public class FileMeta extends BaseEntity {

    /** Người upload. */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /** Dự án chứa file (null với avatar/file không gắn dự án). */
    @Column(name = "project_id")
    private UUID projectId;

    /** Task đính kèm (null nếu không phải attachment). */
    @Column(name = "task_id")
    private UUID taskId;

    /** Tin nhắn đính kèm (null nếu không phải file chat). */
    @Column(name = "message_id")
    private UUID messageId;

    /** Tên file gốc. */
    @Column(nullable = false, length = 255)
    private String name;

    /** MIME type. */
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    /** Kích thước (byte). */
    @Column(nullable = false)
    private long size;

    /** Khoá lưu trữ trong StorageService. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;
}
