package com.example.project.global;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.project.global.exception.CustomException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------
    // 1) 커스텀 예외 처리
    // -------------------------------
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .code(e.getCode())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // -------------------------------
    // 2) RuntimeException 처리
    // -------------------------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .code("RUNTIME_EXCEPTION")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------
    // 3) IllegalStateException 처리
    // -------------------------------
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message(e.getMessage())
                .code("ILLEGAL_STATE")
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------
    // 4) 예상 못한 모든 예외 처리
    // -------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .message("서버 내부 오류가 발생했습니다.")
                .code("INTERNAL_SERVER_ERROR")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // -------------------------------
    // 에러 응답 공통 구조
    // -------------------------------
    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorResponse {
        private boolean success;
        private String message;
        private String code;
    }
}
