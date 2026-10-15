package com.example.remoteapp.common;

import lombok.Getter;

/**
 * Exception nghiệp vụ duy nhất của ứng dụng — service ném ra khi vi phạm luật,
 * {@link GlobalExceptionHandler} chuyển thành response lỗi thống nhất.
 *
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.3
 */
@Getter
public class AppException extends RuntimeException {

    /** Mã lỗi nghiệp vụ. */
    private final ErrorCode errorCode;

    /**
     * Tạo exception với message mặc định của {@link ErrorCode}.
     *
     * @param errorCode mã lỗi nghiệp vụ
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.3
     */
    public AppException(ErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    /**
     * Tạo exception với message tuỳ chỉnh (không lộ thông tin nhạy cảm).
     *
     * @param errorCode mã lỗi nghiệp vụ
     * @param message   mô tả lỗi (tiếng Việt) gửi cho client
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.3
     */
    public AppException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
