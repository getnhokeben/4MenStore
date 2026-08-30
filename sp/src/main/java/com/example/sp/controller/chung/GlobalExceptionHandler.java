package com.example.sp.controller.chung;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Xử lý tương tác người dùng cho handle validation exceptions.
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    // Xử lý tương tác người dùng cho handle invalid json.
    public ResponseEntity<Map<String, String>> handleInvalidJson(HttpMessageNotReadableException ex) {
        Map<String, String> err = new HashMap<>();
        err.put("message", "Nội dung gửi lên không hợp lệ");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(ResponseStatusException.class)
    // Xử lý tương tác người dùng cho handle response status.
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        Map<String, String> err = new HashMap<>();
        err.put("message", ex.getReason() == null ? "Yêu cầu không hợp lệ" : ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(err);
    }

    /**
     * Business-rule validation failures are expected 400 responses. Keep them
     * out of the generic RuntimeException handler so a normal duplicate-field
     * check does not produce a full server stack trace.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    // Xử lý tương tác người dùng cho handle illegal argument.
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> err = new HashMap<>();
        err.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Dữ liệu không hợp lệ"
                : ex.getMessage());
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(RuntimeException.class)
    // Xử lý tương tác người dùng cho handle runtime.
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException ex) {
        log.warn("Yêu cầu API thất bại: {}", ex.getMessage(), ex);
        Map<String, String> err = new HashMap<>();
        err.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Có lỗi xảy ra khi xử lý yêu cầu"
                : ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }
}
