package com.training.PaymentHub.exception;

import com.training.PaymentHub.model.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Authentication Errors (401) ---

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, ex.getMessage()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, ex.getMessage()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "TÃ i khoáº£n Ä‘Ã£ bá»‹ vÃ´ hiá»‡u hÃ³a"));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "TÃ i khoáº£n Ä‘ang bá»‹ khÃ³a"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "XÃ¡c thá»±c tháº¥t báº¡i: " + ex.getMessage()));
    }

    // --- Authorization Errors (403) ---

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(403, "Báº¡n khÃ´ng cÃ³ quyá»n thá»±c hiá»‡n hÃ nh Ä‘á»™ng nÃ y"));
    }

    // --- Validation Errors (400) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, errorMessages));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    // --- Business Logic Errors (RuntimeException -> 400) ---

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    // --- Catch-all (500) ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Lá»—i há»‡ thá»‘ng: " + ex.getMessage()));
    }
}

