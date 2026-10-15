package com.example.remoteapp.common;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Chuyển mọi exception thành response lỗi thống nhất (plan 02 §1.1).
 *
 * @reference docs/plan-backend/02-danh-sach-api.md §1.1
 * @reference docs/plan-backend/01-kien-truc-va-stack.md §4.3
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý exception nghiệp vụ {@link AppException}.
     *
     * @param ex      exception nghiệp vụ
     * @param request request hiện tại (lấy path)
     * @return response lỗi theo format chuẩn
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.from(ex, request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }

    /**
     * Xử lý lỗi validate DTO ({@code @NotBlank}, {@code @Size}, {@code @Email}...).
     *
     * @param ex      exception validate
     * @param request request hiện tại
     * @return response lỗi VALIDATION_ERROR kèm trường vi phạm
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §9
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        ErrorResponse body = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message, request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }

    /**
     * Định dạng lỗi của một trường trong DTO.
     *
     * @param error lỗi field
     * @return chuỗi "field: message"
     * @reference docs/plan-backend/01-kien-truc-va-stack.md §9
     */
    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    /**
     * Xử lý body JSON sai định dạng / sai kiểu dữ liệu.
     *
     * @param ex      exception parse JSON
     * @param request request hiện tại
     * @return response lỗi VALIDATION_ERROR
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Body request không hợp lệ",
                request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }

    /**
     * Xử lý sai kiểu tham số path (ví dụ id không phải UUID).
     *
     * @param ex      exception type mismatch
     * @param request request hiện tại
     * @return response lỗi VALIDATION_ERROR
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Tham số không hợp lệ",
                request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }

    /**
     * Xử lý HTTP method không hỗ trợ.
     *
     * @param ex      exception method
     * @param request request hiện tại
     * @return response lỗi 405
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Phương thức không được hỗ trợ",
                request.getRequestURI());
        return ResponseEntity.status(405).body(body);
    }

    /**
     * Xử lý truy cập bị từ chối (RBAC — role không đủ).
     *
     * @param ex      exception phân quyền
     * @param request request hiện tại
     * @return response lỗi PROJECT_FORBIDDEN
     * @reference docs/04-thiet-ke-backend.md §9
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(ErrorCode.PROJECT_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này",
                request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }

    /**
     * Bắt mọi exception còn lại — log lỗi, trả INTERNAL_ERROR, không lộ stacktrace.
     *
     * @param ex      exception bất kỳ
     * @param request request hiện tại
     * @return response lỗi INTERNAL_ERROR
     * @reference docs/plan-backend/02-danh-sach-api.md §1.1
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ErrorResponse body = ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Lỗi hệ thống", request.getRequestURI());
        return ResponseEntity.status(body.status()).body(body);
    }
}
