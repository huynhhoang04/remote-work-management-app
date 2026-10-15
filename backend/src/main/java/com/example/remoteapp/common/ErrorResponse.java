package com.example.remoteapp.common;

import java.time.Instant;

/**
 * DTO response lỗi thống nhất cho mọi endpoint (plan 02 §1.1).
 *
 * @param timestamp thời điểm phát sinh lỗi
 * @param status    HTTP status
 * @param code      mã lỗi nghiệp vụ (xem {@link ErrorCode})
 * @param message   mô tả lỗi
 * @param path      đường dẫn request
 * @reference docs/plan-backend/02-danh-sach-api.md §1.1
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path) {

    /**
     * Dựng response lỗi từ {@link AppException}.
     *
     * @param ex   exception nghiệp vụ
     * @param path đường dẫn request
     * @return response lỗi theo format chuẩn
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    public static ErrorResponse from(AppException ex, String path) {
        return new ErrorResponse(
                Instant.now(),
                ex.getErrorCode().getHttpStatus(),
                ex.getErrorCode().name(),
                ex.getMessage(),
                path);
    }

    /**
     * Dựng response lỗi từ mã lỗi + message.
     *
     * @param errorCode mã lỗi nghiệp vụ
     * @param message   mô tả lỗi
     * @param path      đường dẫn request
     * @return response lỗi theo format chuẩn
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(
                Instant.now(),
                errorCode.getHttpStatus(),
                errorCode.name(),
                message,
                path);
    }
}
