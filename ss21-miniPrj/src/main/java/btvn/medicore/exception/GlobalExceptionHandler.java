package btvn.medicore.exception;

import btvn.medicore.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j // Khởi tạo SLF4J
@RestControllerAdvice // Kích hoạt AOP bắt lỗi toàn cục
public class GlobalExceptionHandler {

    // Bắt lỗi sai quyền truy cập (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Cảnh báo bảo mật: Truy cập trái phép! Chi tiết: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Bạn không có quyền thực hiện hành động này!",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // Bắt các lỗi RuntimeException nghiệp vụ chung
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Lỗi hệ thống: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Bắt lỗi token hết hạn (Hoặc bất kỳ Exception nào khác)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Lỗi Server nghiêm trọng: ", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Đã xảy ra lỗi trên máy chủ. Vui lòng thử lại sau!",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(io.jsonwebtoken.ExpiredJwtException ex) {
        log.error("Cảnh báo: Access Token đã hết hạn! Client cần dùng Refresh Token. Chi tiết: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(), // Trả về đúng 401 Unauthorized
                "Access Token đã hết hạn. Vui lòng gọi API /refresh để cấp lại!",
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }
}